
import pandas as pd
import geopandas as gpd
import statsmodels.formula.api as smf
import numpy as np
#import osmnx as ox
import pickle
from scipy.optimize import least_squares
from shapely.geometry import  Point
import itertools
import csv
import yaml
#%% Load TAZ data from MTC

taz = gpd.read_file('data/Transportation_Analysis_Zones.shp')
taz['taz_id'] = taz['taz1454']
taz = taz.set_index('taz1454', drop=True)
demo = pd.read_csv('data/Plan_Bay_Area_2040_Forecast__Population_and_Demographics.csv').set_index('zoneid',drop=True)
emp = pd.read_csv('data/Plan_Bay_Area_2040_Forecast__Employment.csv').set_index('zoneid',drop=True)
lut = pd.read_csv('data/Plan_Bay_Area_2040_Forecast__Land_Use_and_Transportation.csv').set_index('zoneid',drop=True)
taz = taz.merge(demo, left_index=True,right_index=True).merge(emp, left_index=True,right_index=True).merge(lut, left_index=True,right_index=True)
taz = pd.concat([taz.iloc[:,:8],taz.loc[:,taz.columns.str.endswith('15')]],axis=1)
taz['area'] = taz['geometry'].to_crs({'init': 'epsg:3395'}).area/10**3 # in 1000s of sq meters (good numerically)
sf_boundary = taz.loc[taz['county'] == 'San Francisco','geometry'].unary_union
cols = taz.columns
for col in cols[[8,20,21,22,23,24,25,26,27,31,32]]:
    taz.loc[taz[col] == 0,col] += 0.1
    taz[col+'_den'] = taz[col]/taz['area']

taz['AreaType'] = 'Residential'
taz.loc[taz['areatype15'] == 0,'AreaType'] = 'CBD'
taz.loc[taz['areatype15'] == 1,'AreaType'] = 'Downtown'
taz.loc[taz['areatype15'] == 2,'AreaType'] = 'Downtown'
taz.loc[taz['areatype15'] == 3,'AreaType'] = 'Residential'
taz['JobsAndResidents'] = taz['totemp15'] + taz['totpop15']
taz['JobsAndResidents_den'] = taz['JobsAndResidents'] / taz['area']


#%%


hh = pd.read_csv('data/households.csv.gz')
buildings = pd.read_csv('data/buildings.csv.gz')
parcels = pd.read_csv('data/parcels.csv.gz')
buildings = buildings[['building_id','parcel_id']].merge(parcels[['parcel_id','x','y']],left_on='parcel_id',right_on='parcel_id')
hh = hh.merge(buildings,left_on='building_id',right_on='building_id')
hh = gpd.GeoDataFrame(hh, geometry=gpd.points_from_xy(hh.x, hh.y))
hh.crs = {'init': 'epsg:4326'}
hh_with_taz = gpd.sjoin(taz,hh[['household_id','cars','workers','persons','single_family','geometry']],how='inner',op='intersects')
sfhh = hh_with_taz.loc[hh_with_taz['single_family'],:]
mfhh = hh_with_taz.loc[~hh_with_taz['single_family'],:]
del hh
del buildings
del parcels
mf_cars = mfhh.groupby(mfhh.index).agg({'cars':'sum','persons':'sum','household_id':'count'}).rename(columns={"cars": "mf_cars", "persons":"mf_persons", "household_id": "mfhh"})
sf_cars = sfhh.groupby(sfhh.index).agg({'cars':'sum','persons':'sum','household_id':'count'}).rename(columns={"cars": "sf_cars", "persons":"sf_persons", "household_id": "sfhh"})
del sfhh
del mfhh
taz = taz.merge(mf_cars, left_index=True, right_index=True).merge(sf_cars, left_index=True, right_index=True)
taz['cars_per_mfhh'] = taz['mf_cars']/taz['mfhh']
taz['cars_per_sfhh'] = taz['sf_cars']/taz['sfhh']
taz['cars'] = taz['mf_cars'] + taz['sf_cars']
taz['hh'] = taz['sfhh'] + taz['mfhh']
taz['pop'] = taz['mf_persons'] + taz['sf_persons']
#%% Load BART distances

bart_distances = pd.read_csv('data/distanceToBart.csv').set_index('InputID')
taz = taz.merge(bart_distances,left_index=True,right_index=True,how='left')
taz.rename(columns={'Distance':'DistanceToBART'},inplace=True)
taz['NearBart'] = taz['DistanceToBART'] < 800

#%% Load OSM data and group road segments by TAZ

with open('gdfs.pickle', 'rb') as f:
    gdfs = pickle.load(f)

def center_of_linestring(x):
    return Point(np.mean(x.coords.xy[0]),np.mean(x.coords.xy[1]))
gdfs['geometry'] = gdfs['geometry'].apply(center_of_linestring)
gdfs['oneway'] = gdfs['oneway'].str == 'True'
gdfs.loc[~gdfs['oneway'],'length_corrected'] = gdfs.loc[~gdfs['oneway'],'length']/2
gdfs['classification'] = gdfs['highway'].str.replace('_link','').replace('living_street','unclassified')

gdfs.rename(columns={'length':'length_OSM','length_corrected':'length_corrected_OSM'},inplace=True)
OSM_with_taz = gpd.sjoin(taz,gdfs[['oneway','geometry','length_OSM','length_corrected_OSM','classification']],how='inner',op='contains')
taz_with_OSM = OSM_with_taz.groupby([OSM_with_taz.index,OSM_with_taz['classification']]).agg({'oneway':'sum','length_OSM':'sum','length_corrected_OSM':'sum'}).unstack(level=-1).fillna(0)
taz_with_OSM.columns = taz_with_OSM.columns.to_flat_index().map('_'.join)

#%% Group On Street Parking by TAZ

onstp = gpd.read_file('data/OnStreetParking/geo_export_9c00c3f8-0452-4427-add0-07e5c897479a.shp')
ECKERT_IV_PROJ4_STRING = "+proj=eck4 +lon_0=0 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs"
onstp['length'] = onstp['geometry'].to_crs(ECKERT_IV_PROJ4_STRING).length
onstp['geometry'] = onstp['geometry'].apply(center_of_linestring)
onstp_with_taz = gpd.sjoin(taz,onstp[['prkg_sply','geometry','length']],how='inner',op='intersects')
taz_with_onstp = onstp_with_taz.groupby(onstp_with_taz.index).agg({'prkg_sply':'sum','length':'sum'}).fillna(0)
taz_with_onstp.rename(columns={'length':'length_SF','prkg_sply':'OnStreetParking'},inplace=True)

#%% Group Off Street Parking by TAZ
offstp = gpd.read_file('data/OffStreetParking/OSP_09162011.shp')
offstp['OffStreetParking'] = offstp['RegCap'] + offstp['ValetCap']
offstp['PaidPublicParking'] = offstp['OffStreetParking'] * (offstp['PrimeType'] == 'PPA')
offstp['FreePublicParking'] = offstp['OffStreetParking'] * ((offstp['PrimeType'] == 'FPA') | (offstp['PrimeType'] == 'CPO'))
offstp['WorkParking'] = offstp['OffStreetParking'] * ((offstp['PrimeType'] == 'PHO') | (offstp['PrimeType'] == 'CGO'))



offstp_with_taz = gpd.sjoin(taz,offstp[['RegCap','ValetCap','MCCap','OffStreetParking','PaidPublicParking','FreePublicParking','WorkParking','geometry']],how='inner',op='intersects')
taz_with_offstp = offstp_with_taz.groupby(offstp_with_taz.index).agg({'objectid':'first','OffStreetParking':'sum','PaidPublicParking':'sum','FreePublicParking':'sum','WorkParking':'sum'}).fillna(0)

#%% Group Parking Meters by TAZ

parking_meters = pd.read_csv('data/OnStreetParking/Parking_Meters.csv')
parking_meters = parking_meters.loc[parking_meters['ON_OFFSTREET_TYPE']=='ON',:]
parking_meters = gpd.GeoDataFrame(parking_meters,geometry=gpd.points_from_xy(parking_meters['LONGITUDE'],parking_meters['LATITUDE']))
parking_meters.crs = {'init': 'epsg:4326'}
meters_with_taz = gpd.sjoin(taz,parking_meters[['OBJECTID','geometry']],how='inner',op='intersects')
taz_with_meters = meters_with_taz.groupby(meters_with_taz.index).agg({'OBJECTID':'count'}).fillna(0)
taz_with_meters.rename(columns={'OBJECTID':'ParkingMeters'},inplace=True)


#%%
taz_all = taz.merge(taz_with_onstp,left_index=True,right_index=True,how='left')
taz_all = taz_all.merge(taz_with_OSM,left_index=True,right_index=True,how='left')
taz_all = taz_all.merge(taz_with_offstp,left_index=True,right_index=True,how='left')
taz_all = taz_all.merge(taz_with_meters, left_index=True, right_index = True, how='left')


taz_all['AllParking'] = (taz_all['OffStreetParking'] + taz_all['OnStreetParking'])
taz_all['AllParking_den'] = taz_all['AllParking'] / taz_all['area']
taz_all['OffStreetParking_den'] = taz_all['OffStreetParking']/ taz_all['area']
taz_all['OnStreetParking_den'] = taz_all['OnStreetParking']/ taz_all['area']
taz_all['PortionOnStreet'] = taz_all['OnStreetParking'] / (taz_all['AllParking'] + 0.1 )
taz_all['PortionOnStreetPaid'] = taz_all['ParkingMeters'] / (taz_all['OnStreetParking'] + 0.1)
taz_all['PortionOffStreetPaid'] = taz_all['PaidPublicParking'] / (taz_all['OffStreetParking'] + 0.1)
#taz_all.loc[taz_all['PortionOffStreetPaid'] > 1,'PortionOffStreetPaid'] = 1 # Why do these exist?
taz_all.loc[taz_all['PortionOnStreetPaid'] > 1, 'PortionOnStreetPaid'] = 1
taz_all.replace(np.nan, 0,inplace=True)
#%% Get Just SF data to train models

sf = taz_all.loc[taz['county']=='San Francisco',:]
sf['OnStreetParkingPerDistance'] = sf['OnStreetParking']/sf['length_SF']
sf['JobsPerOffStreetParking'] = sf['totemp15']/ (sf['OffStreetParking'] + 1)
sf['JobsPerOnStreetParking'] = sf['totemp15']/ (sf['OnStreetParking'] + 1)
sf['OnStreetParkingPerJob'] = sf['OnStreetParking'] /(sf['totemp15'] + 1)
sf['OffStreetParkingPerJob'] = sf['OffStreetParking'] /(sf['totemp15'] + 1)


sf = sf.loc[sf.index != 1258,:]# Get rid of candlestick park (outlier high off street parking)
sf = sf.loc[sf.index != 1074,:]# Get rid of treasure island (outlier low on street parking, not measured maybe)

sf['AllParkingPerJobAndResident'] = sf['AllParking'] / sf['JobsAndResidents']

sf['OffStreetParkingPerJobAndResident'] = sf['OffStreetParking'] / sf['JobsAndResidents']

sf['OnStreetParkingPerJobAndResident'] = sf['OnStreetParking'] / sf['JobsAndResidents']

sf['OffStreetParkingPerJob'] = sf['OffStreetParking'] / sf['totemp15']

sf['JobPerOffStreetParking'] =  sf['totemp15'] / sf['OffStreetParking']

sf['AllParkingPerCar'] = sf['AllParking'] / (sf['totemp15'] + sf['cars'])

#%%


onstreet_simple = smf.ols(formula = 'np.log(OffStreetParking_den) ~ np.log(totemp15_den)', data = sf.loc[(sf['AllParkingPerJobAndResident'] > 0) & (sf['totemp15_den'] > 0.5)])
onstreet_simple_res = onstreet_simple.fit()
print(onstreet_simple_res.summary())

onstreet_simple = smf.ols(formula = 'np.log(OnStreetParkingPerJobAndResident) ~ np.log(JobsAndResidents_den)', data = sf.loc[sf['AllParkingPerJobAndResident'] > 0])
onstreet_simple_res = onstreet_simple.fit()
print(onstreet_simple_res.summary())

onstreet_simple = smf.ols(formula = 'oprkcst15 ~ np.log(totemp15_den) + np.log(totpop15_den)', data = sf.loc[sf['AllParkingPerJobAndResident'] > 0])
onstreet_simple_res = onstreet_simple.fit()
print(onstreet_simple_res.summary())

onstreet_simple = smf.ols(formula = 'prkcst15 ~ np.log(totemp15_den) + np.log(totpop15_den)', data = sf.loc[sf['AllParkingPerJobAndResident'] > 0])
onstreet_simple_res = onstreet_simple.fit()
print(onstreet_simple_res.summary())


onstreet_simple = smf.ols(formula = 'OnStreetParkingPerJobAndResident ~ np.log(totemp15_den+totpop15_den)', data = sf.loc[(sf['AllParkingPerJobAndResident'] > 0) & (sf['totemp15_den'] > 0.5)])
onstreet_simple_res = onstreet_simple.fit()
print(onstreet_simple_res.summary())


onstreet_simple = smf.ols(formula = 'oprkcst15 ~ totemp15_den + totpop15_den', data = sf.loc[sf['AllParkingPerJobAndResident'] > 0])
onstreet_simple_res = onstreet_simple.fit()
print(onstreet_simple_res.summary())

onstreet_simple = smf.ols(formula = 'prkcst15 ~ totemp15_den + totpop15_den', data = sf.loc[sf['AllParkingPerJobAndResident'] > 0])
onstreet_simple_res = onstreet_simple.fit()
print(onstreet_simple_res.summary())

#%% Train On Steet Parking Model

onstreet_simple = smf.ols(formula = 'OnStreetParking ~  -1 + length_OSM_primary + length_OSM_residential + length_OSM_secondary +length_OSM_trunk', data = sf)
onstreet_simple_res = onstreet_simple.fit()
print(onstreet_simple_res.summary())

# TODO: Add back in motorway once there is internet
onstreet_full = smf.ols(formula = 'OnStreetParking ~  -1 + length_OSM_tertiary+ length_OSM_primary + length_OSM_residential + length_OSM_secondary +length_OSM_trunk', data = sf)
onstreet_full_res = onstreet_full.fit()
print(onstreet_full_res.summary())

#%% Train On Street Paid Parking Model

metered_simple = smf.logit(formula = 'PortionOnStreetPaid ~ np.log(totemp15_den)', data = sf)
metered_simple_res = metered_simple.fit()
print(metered_simple_res.summary())

metered_full = smf.logit(formula = 'PortionOnStreetPaid ~ np.log(totemp15_den):C(AreaType) + np.log(sfdu15_den) + np.log(mfdu15_den)', data = sf)
metered_full_res = metered_full.fit()
print(metered_full_res.summary())

#%% Train Off Street Model

def fun(params, inputs, output):
    return np.sum(inputs[:,:-2]*params[:-3],axis=1)/(1 + np.exp(- params[-3]+ params[-2]*inputs[:,-2]  + params[-1]*inputs[:,-1] )) - output

inputs = np.vstack([sf['retempn15_den'],sf['fpsempn15_den'],sf['herempn15_den'],sf['othempn15_den'], sf['mfdu15_den'], sf['sfdu15_den'], np.log(sf['totemp15_den']), np.log(sf['totpop15_den'])]).transpose()
outputs = sf['OffStreetParking_den'].values
x0 = np.ones(9)

off_street_full = least_squares(fun, x0, loss='linear', f_scale=5, args=(inputs, outputs))
print('Complex Model Params', off_street_full.x)
print('Complex Model Cost', off_street_full.cost)

def fun_simple(params, inputs, output):
    return np.sum(inputs[:,:-2]*params[:-3],axis=1)/(1 + np.exp(- params[-3] + params[-2]*inputs[:,-2]  + params[-1]*inputs[:,-1] )) - output

inputs = np.vstack([sf['totemp15_den'],np.log(sf['totemp15_den']), np.log(sf['totpop15_den'])]).transpose()
outputs = sf['OffStreetParking_den'].values
x0 = np.ones(4)

off_street_simple = least_squares(fun_simple, x0, loss='linear', f_scale=5, args=(inputs, outputs))
print('Simple Model Params', off_street_simple.x)
print('Simple Model Cost', off_street_simple.cost)



#%% Train Off Street Paid Model
off_street_paid_full = smf.logit(formula = 'PortionOffStreetPaid ~ C(AreaType) + np.log(retempn15_den) + np.log(fpsempn15_den) + np.log(herempn15_den) + np.log(mwtempn15_den) + np.log(othempn15_den) + np.log(sfdu15_den):C(AreaType) + np.log(mfdu15_den)', data = sf)
off_street_paid_full_res = off_street_paid_full.fit()
print(off_street_paid_full_res.summary())


off_street_paid_simple = smf.logit(formula = 'PortionOffStreetPaid ~ np.log(totemp15_den) + np.log(totpop15_den) ', data = sf)
off_street_paid_res = off_street_paid_simple.fit()
print(off_street_paid_res.summary())

#%% Train Short Term Hourly Rate Model

st_parking_cost = smf.ols(formula = 'oprkcst15 ~  np.log(totemp15_den) + np.log(totpop15_den)', data = sf)
st_parking_cost_res = st_parking_cost.fit()
print(st_parking_cost_res.summary())

#%% Train Long Term Hourly Rate Model

lt_parking_cost = smf.ols(formula = 'prkcst15 ~  np.log(totemp15_den) + np.log(totpop15_den)', data = sf)
lt_parking_cost_res = lt_parking_cost.fit()
print(lt_parking_cost_res.summary())

#%% Fit Models for SFBAY

taz_all = taz_all.loc[taz_all.index.drop_duplicates(keep=False),:]

# On Street
taz_all['PredictedOnStreetParking'] = onstreet_full_res.predict(taz_all)

# On Street Paid
taz_all['PredictedOnStreetPortionPaid'] = metered_full_res.predict(taz_all)
taz_all['PredictedOnStreetPaidParking'] = np.ceil(taz_all['PredictedOnStreetParking'] * taz_all['PredictedOnStreetPortionPaid'])
taz_all['PredictedOnStreetFreeParking'] = np.ceil(taz_all['PredictedOnStreetParking'] - taz_all['PredictedOnStreetPaidParking'])

# Off Street
def predict_off_street(params, inputs):
    return np.sum(inputs[:,:-2]*params[:-3],axis=1)/(1 + np.exp(- params[-3]+ params[-2]*inputs[:,-2]  + params[-1]*inputs[:,-1] ))

full_inputs = np.vstack([taz_all['retempn15_den'],taz_all['fpsempn15_den'],taz_all['herempn15_den'],taz_all['othempn15_den'], taz_all['mfdu15_den'], taz_all['sfdu15_den'], np.log(taz_all['totemp15_den']), np.log(taz_all['totpop15_den'])]).transpose()
taz_all['PredictedOffStreetParking'] = predict_off_street(off_street_full.x,full_inputs) * taz_all['area']
taz_all.loc[taz_all['PredictedOffStreetParking'] < 0,'PredictedOffStreetParking'] = 0
taz_all['PredictedOffStreetParking_extra'] = taz_all['PredictedOffStreetParking'] + 0.5*taz_all['mwtempn15'] + 1.0*taz_all['agrempn15'] # Make sure manufacturing and ag jobs have places to park (not well represented in SF)

# Off Street Paid
taz_all['PredictedOffStreetPortionPaid'] = off_street_paid_full_res.predict(taz_all)
taz_all['PredictedOffStreetPaidParking'] = np.ceil(taz_all['PredictedOffStreetParking_extra'] * taz_all['PredictedOffStreetPortionPaid'])
taz_all['PredictedOffStreetFreeParking'] = np.ceil(taz_all['PredictedOffStreetParking_extra'] - taz_all['PredictedOffStreetPaidParking'])

# =============================================================================
# # Costs: To compare predictions from just SF to inputs from entire bay area
# taz_all['ShortTermHourlyRate'] = st_parking_cost_res.predict(taz_all)
# taz_all.loc[taz_all['ShortTermHourlyRate'] < 0, 'ShortTermHourlyRate'] = 0
# 
# taz_all['LongTermHourlyRate'] = lt_parking_cost_res.predict(taz_all)
# taz_all.loc[taz_all['LongTermHourlyRate'] < 0, 'LongTermHourlyRate'] = 0
#
# sns.scatterplot(x='LongTermHourlyRate', y='prkcst15', hue='AreaType', data=taz_all)
# =============================================================================
taz_all.loc[taz_all['areatype15'] == 4,'AreaType'] = 'Suburbs' # None of these in SF so they screw up the model


taz_all['MadeUpWorkSpaces'] = 0 # If no area type available, add when employment density is greater than 2?
taz_all.loc[taz_all['AreaType'] == 'Suburbs','MadeUpWorkSpaces'] = taz_all['totemp15']
taz_all.loc[taz_all['AreaType'] == 'Residential','MadeUpWorkSpaces'] = taz_all['totemp15']

taz_all['MadeUpResidentialSpaces'] = 0
taz_all.loc[taz_all['AreaType'] == 'Suburbs','MadeUpResidentialSpaces'] = np.ceil(3*(taz_all['sfdu15'] + taz_all['mfdu15']))
taz_all.loc[taz_all['AreaType'] == 'Residential','MadeUpResidentialSpaces'] = np.ceil(1.5*taz_all['sfdu15'] + 0.75*taz_all['mfdu15'])
taz_all.loc[taz_all['AreaType'] == 'Downtown','MadeUpResidentialSpaces'] = np.ceil(taz_all['sfdu15'] + 0.5*taz_all['mfdu15'])
taz_all.loc[taz_all['AreaType'] == 'CBD','MadeUpResidentialSpaces'] = np.ceil(0.5*taz_all['sfdu15'] + 0.25*taz_all['mfdu15'])


#%% Build scare parking:

scarce_public = pd.read_csv('data/AFI/public-20190815.csv') 
scarce_public = gpd.GeoDataFrame(scarce_public,geometry=gpd.points_from_xy(scarce_public['Longitude'],scarce_public['Latitude']))

scarce_public.crs = {'init': 'epsg:4326'}
scarce_with_taz = gpd.sjoin(taz,scarce_public[['Number of plugs / parking stalls','geometry']],how='inner',op='intersects')
taz_with_scarce = scarce_with_taz.groupby(scarce_with_taz.index).agg({'Number of plugs / parking stalls':'sum'}).fillna(0)
taz_with_scarce.rename(columns={'Number of plugs / parking stalls':'ScarcePublicPlugs'},inplace=True)

taz_all = taz_all.merge(taz_with_scarce['ScarcePublicPlugs'], left_index=True, right_index = True, how='left').fillna(0)

scarce_depot = pd.read_csv('data/AFI/depot-20190815.csv') 
scarce_depot = gpd.GeoDataFrame(scarce_depot,geometry=gpd.points_from_xy(scarce_depot['Longitude'],scarce_depot['Latitude']))

scarce_depot.crs = {'init': 'epsg:4326'}
scarce_with_taz = gpd.sjoin(taz,scarce_depot[['Number of plugs / parking stalls','geometry']],how='inner',op='intersects')
taz_with_scarce = scarce_with_taz.groupby(scarce_with_taz.index).agg({'Number of plugs / parking stalls':'sum'}).fillna(0)
taz_with_scarce.rename(columns={'Number of plugs / parking stalls':'ScarceDepotPlugs'},inplace=True)


taz_all = taz_all.merge(taz_with_scarce['ScarceDepotPlugs'], left_index=True, right_index = True, how='left').fillna(0)
taz_all['PredictedOffStreetPaidParkingCorrected'] = taz_all['PredictedOffStreetPaidParking'] - taz_all['ScarcePublicPlugs']
taz_all.loc[taz_all['PredictedOffStreetPaidParkingCorrected'] < 0,'PredictedOffStreetPaidParkingCorrected'] = 0

#%% LOOK AT URBANSIM:
scenarios = ['baseline','lowtech-a','hightech-a','lowtech-b','hightech-b','lowtech-c','hightech-c']
retirement = [0.0, 0.45, 0.45, 0.68, 0.75, 0.15, 0.20]
ev = [0.001, 0.041667, 0.072768, 0.116822, 0.301087, 0.168935, 0.397117]
phev = [0.001000, 0.031246, 0.051985, 0.023726, 0.046324, 0.064920, 0.112313]
eviPublic = ['300mi-150kw-fleet1-a-hightech.PUBLIC.STATIONS.csv','300mi-150kw-fleet1-a-hightech.PUBLIC.STATIONS.csv',
           '300mi-150kw-fleet1-a-hightech.PUBLIC.STATIONS.csv','300mi-150kw-fleet3-b-lowtech.PUBLIC.STATIONS.csv',
           '300mi-150kw-fleet3-b-lowtech.PUBLIC.STATIONS.csv','300mi-150kw-fleet3-b-lowtech.PUBLIC.STATIONS.csv',
           '300mi-150kw-fleet3-b-lowtech.PUBLIC.STATIONS.csv']
eviWork = ['300mi-150kw-fleet1-a-hightech.WORKPLACE.STATIONS.csv','300mi-150kw-fleet1-a-hightech.WORKPLACE.STATIONS.csv',
           '300mi-150kw-fleet1-a-hightech.WORKPLACE.STATIONS.csv','300mi-150kw-fleet3-b-lowtech.WORKPLACE.STATIONS.csv',
           '300mi-150kw-fleet3-b-lowtech.WORKPLACE.STATIONS.csv','300mi-150kw-fleet3-b-lowtech.WORKPLACE.STATIONS.csv',
           '300mi-150kw-fleet3-b-lowtech.WORKPLACE.STATIONS.csv']
scenario_data = pd.DataFrame({'retirement':retirement,'ev':ev,'phev':phev,'eviPublic':eviPublic, 'eviWork':eviWork}, index=scenarios)

taz_all['sf_vehs'] = taz_all['sfdu15'] * taz_all['cars_per_sfhh']
taz_all['mf_vehs'] = taz_all['mfdu15'] * taz_all['cars_per_mfhh']
#%% Build output file

sample=1.0
infrastructure_sample = 0.1

vehiclesWithWorkplaceCharging = 0.9*0.08

costPerkWh_DCfast = 2.
costPerkWh_Public2 = 2.
costPerkWh_Res2 = 0.25
costPerkWh_Res1 = 0.25

def process_evipro(prefix, filename):
    output = []
    evi_100 = pd.read_csv(prefix + '1' + filename[1:])
    evi_300 = pd.read_csv(prefix + '3' + filename[1:])
    for row in evi_100.itertuples():
        chargerDict = yaml.safe_load(row.plug_summary)
        newrow = {'Latitude':row.Latitude,'Longitude':row.Longitude}
        if 'DCFC' in chargerDict:
            newrow['numFastChargers'] = chargerDict['DCFC']/infrastructure_sample/2
        else:
            newrow['numFastChargers'] = 0
        if 'L2' in chargerDict:
            newrow['numSlowChargers'] = chargerDict['L2']/infrastructure_sample/2
        else:
            newrow['numSlowChargers'] = 0
        output.append(newrow)
    for row in evi_300.itertuples():
        chargerDict = yaml.safe_load(row.plug_summary)
        newrow = {'Latitude':row.Latitude,'Longitude':row.Longitude}
        if 'DCFC' in chargerDict:
            newrow['numFastChargers'] = chargerDict['DCFC']/infrastructure_sample/2
        else:
            newrow['numFastChargers'] = 0
        if 'L2' in chargerDict:
            newrow['numSlowChargers'] = chargerDict['L2']/infrastructure_sample/2
        else:
            newrow['numSlowChargers'] = 0
        output.append(newrow)
    df = pd.DataFrame(output)
    return gpd.GeoDataFrame(df,geometry=gpd.points_from_xy(df['Longitude'],df['Latitude']))


output = []

for idx, row in taz_all.iterrows():
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'DCfast(150.0|DC)','numStalls':np.ceil(row.PredictedOffStreetPaidParking*sample),'feeInCents':row.prkcst15+150*costPerkWh_DCfast,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'DCfast(150.0|DC)','numStalls':np.ceil(row.PredictedOffStreetFreeParking*sample),'feeInCents':150*costPerkWh_DCfast,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Level2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetPaidParking*sample),'feeInCents':row.prkcst15+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Level2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetFreeParking*sample),'feeInCents':7.2*costPerkWh_Public2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'Level2(7.2|AC)','numStalls':np.ceil(row.MadeUpResidentialSpaces*sample),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'Level1(1.8|AC)','numStalls':np.ceil(row.MadeUpResidentialSpaces*sample),'feeInCents':1.8*costPerkWh_Res1,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'DCfast(150.0|DC)','numStalls':np.ceil(row.MadeUpWorkSpaces*sample),'feeInCents':150*costPerkWh_DCfast,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'Level2(7.2|AC)','numStalls':np.ceil(row.MadeUpWorkSpaces*sample),'feeInCents':7.2*costPerkWh_Public2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'Level1(1.8|AC)','numStalls':np.ceil(row.MadeUpWorkSpaces*sample),'feeInCents':1.8*costPerkWh_Public2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetPaidParking*sample),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetFreeParking*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParking*sample),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpResidentialSpaces*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/taz-parking-base-unlimitedcharging-150.csv',index=False,quotechar='\'')

power = 150

for scenario in scenarios:
    taz_scenario = taz_all.copy()
    evs_as_fraction_of_initial_fleet = (scenario_data.loc[scenario,'ev'] + scenario_data.loc[scenario,'phev']) * (1.0 - scenario_data.loc[scenario,'retirement'])
    taz_scenario['sf_pevs'] = taz_scenario['sf_vehs'] * evs_as_fraction_of_initial_fleet
    taz_scenario['mf_pevs'] = taz_scenario['mf_vehs'] * evs_as_fraction_of_initial_fleet
    taz_scenario['home_l1'] = 0.3 * taz_scenario['sf_pevs'] + 0.2 * taz_scenario['mf_pevs']
    taz_scenario['home_l2'] = 0.6 * taz_scenario['sf_pevs'] + 0.3 * taz_scenario['mf_pevs']
    taz_scenario['residential_nocharger'] = taz_scenario['MadeUpResidentialSpaces'] - taz_scenario['home_l1'] - taz_scenario['home_l2']
    work_spot_charger_fraction = evs_as_fraction_of_initial_fleet * vehiclesWithWorkplaceCharging
    
    evi = process_evipro('evipro-stations-190820/',scenario_data.loc[scenario,'eviPublic'])
    
    evi_with_taz = gpd.sjoin(taz_scenario,evi[['numSlowChargers','numFastChargers','geometry']],how='inner',op='intersects')
    taz_with_evi = evi_with_taz.groupby(evi_with_taz.index).agg({'numSlowChargers':'sum','numFastChargers':'sum'}).fillna(0)
    taz_scenario = pd.merge(taz_scenario,taz_with_evi[['numSlowChargers','numFastChargers']], left_index=True, right_index = True, how='left').fillna(0).rename(columns={'numSlowChargers':'numSlowChargersPublic','numFastChargers':'numFastChargersPublic'})
    
    evi = process_evipro('evipro-stations-190820/',scenario_data.loc[scenario,'eviWork'])
    
    evi_with_taz = gpd.sjoin(taz_scenario,evi[['numSlowChargers','numFastChargers','geometry']],how='inner',op='intersects')
    taz_with_evi = evi_with_taz.groupby(evi_with_taz.index).agg({'numSlowChargers':'sum','numFastChargers':'sum'}).fillna(0)
    taz_scenario = pd.merge(taz_scenario,taz_with_evi[['numSlowChargers','numFastChargers']], left_index=True, right_index = True, how='left').fillna(0).rename(columns={'numSlowChargers':'numSlowChargersWork','numFastChargers':'numFastChargersWork'})
     
    output = []
    
    for idx, row in taz_scenario.iterrows():
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.PredictedOnStreetPaidParking),'feeInCents':row.oprkcst15+150*costPerkWh_DCfast,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.PredictedOnStreetFreeParking),'feeInCents':0+510*costPerkWh_DCfast,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.PredictedOffStreetPaidParking),'feeInCents':row.prkcst15+150*costPerkWh_DCfast,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.PredictedOffStreetFreeParking),'feeInCents':0+150*costPerkWh_DCfast,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'Custom(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.MadeUpResidentialSpaces),'feeInCents':0+150*costPerkWh_DCfast,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'Custom(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.MadeUpWorkSpaces),'feeInCents':0+150*costPerkWh_DCfast,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetPaidParking),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetFreeParking),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParking),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.residential_nocharger),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParking * (1.0 - work_spot_charger_fraction)),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking * (1.0 - work_spot_charger_fraction)),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces * (1.0 - work_spot_charger_fraction)),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetPaidParking * (work_spot_charger_fraction)),'feeInCents':row.prkcst15+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetFreeParking * (work_spot_charger_fraction)),'feeInCents':0+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.MadeUpWorkSpaces * (work_spot_charger_fraction)),'feeInCents':0+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'HomeLevel2(7.2|AC)','numStalls':np.ceil(row.home_l2),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'HomeLevel1(1.8|AC)','numStalls':np.ceil(row.home_l1),'feeInCents':1.8*costPerkWh_Res1,'ReservedFor':'Any'}
        output.append(newrow)
        
    output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
    output = output.loc[output['numStalls'] > 0, :]
    output['numStalls'] = output['numStalls'].astype('int')
    output.to_csv('output/taz-parking-unlimited-fast-limited-l2-'+str(int(power)) +'-'+scenario+'.csv',index=False,quotechar='\'')
    print('Unlimited '+scenario)
    grouped = output.groupby('chargingType').agg('sum')*0.14
    print(grouped)
    print('----------')
    output = []
    
    for idx, row in taz_scenario.iterrows():
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.ScarcePublicPlugs),'feeInCents':row.prkcst15+150*costPerkWh_DCfast,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetPaidParking),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetFreeParking),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParkingCorrected),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.residential_nocharger),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParking * (1.0 - work_spot_charger_fraction)),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking * (1.0 - work_spot_charger_fraction)),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces * (1.0 - work_spot_charger_fraction)),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetPaidParking * (work_spot_charger_fraction)),'feeInCents':row.prkcst15+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetFreeParking * (work_spot_charger_fraction)),'feeInCents':0+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.MadeUpWorkSpaces * (work_spot_charger_fraction)),'feeInCents':0+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'HomeLevel2(7.2|AC)','numStalls':np.ceil(row.home_l2),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'HomeLevel1(1.8|AC)','numStalls':np.ceil(row.home_l1),'feeInCents':1.8*costPerkWh_Res1,'ReservedFor':'Any'}
        output.append(newrow)
        
    output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
    output = output.loc[output['numStalls'] > 0, :]
    output['numStalls'] = output['numStalls'].astype('int')
    output.to_csv('output/taz-parking-sparse-fast-limited-l2-'+str(int(power)) +'-'+scenario+'.csv',index=False,quotechar='\'')
    print('Sparse '+scenario)
    grouped = output.groupby('chargingType').agg('sum')*0.14
    print(grouped)
    print('----------')
    output = []
    
    for idx, row in taz_scenario.iterrows():
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetPaidParking),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetFreeParking),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParkingCorrected),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.residential_nocharger),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParking ),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking ),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces ),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        
        #newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetPaidParking * (work_spot_charger_fraction)),'feeInCents':row.prkcst15+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        #output.append(newrow)
        #newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetFreeParking * (work_spot_charger_fraction)),'feeInCents':0+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        #output.append(newrow)
        #newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.MadeUpWorkSpaces * (work_spot_charger_fraction)),'feeInCents':0+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        #output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'HomeLevel2(7.2|AC)','numStalls':np.ceil(row.home_l2),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'HomeLevel1(1.8|AC)','numStalls':np.ceil(row.home_l1),'feeInCents':1.8*costPerkWh_Res1,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'EVI_Public_DCFast(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.numFastChargersPublic),'feeInCents':150*costPerkWh_DCfast + row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'EVI_Public_Level2(7.2|AC)','numStalls':np.ceil(row.numSlowChargersPublic),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'EVI_Work_DCFast(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.numFastChargersWork),'feeInCents':150*costPerkWh_DCfast,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'EVI_Work_Level2(7.2|AC)','numStalls':np.ceil(row.numSlowChargersWork),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
    output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
    output = output.loc[output['numStalls'] > 0, :]
    output['numStalls'] = output['numStalls'].astype('int')
    output.to_csv('output/taz-parking-rich-200mi-fast-limited-l2-'+str(int(power)) +'-'+scenario+'.csv',index=False,quotechar='\'')
    print('Rich '+scenario)
    grouped = output.groupby('chargingType').agg('sum')*0.14
    print(grouped)
    print('----------')

    output = []
    
    for idx, row in taz_scenario.iterrows():
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetPaidParking),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetFreeParking),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParkingCorrected),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.residential_nocharger),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParking ),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking ),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces ),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        
        #newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetPaidParking * (work_spot_charger_fraction)),'feeInCents':row.prkcst15+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        #output.append(newrow)
        #newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetFreeParking * (work_spot_charger_fraction)),'feeInCents':0+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        #output.append(newrow)
        #newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.MadeUpWorkSpaces * (work_spot_charger_fraction)),'feeInCents':0+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        #output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'HomeLevel2(7.2|AC)','numStalls':np.ceil(row.home_l2),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'HomeLevel1(1.8|AC)','numStalls':np.ceil(row.home_l1),'feeInCents':1.8*costPerkWh_Res1,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'EVI_Public_DCFast(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.numFastChargersPublic/10.),'feeInCents':power*costPerkWh_DCfast + row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'EVI_Public_Level2(7.2|AC)','numStalls':np.ceil(row.numSlowChargersPublic/10.),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'EVI_Work_DCFast(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.numFastChargersWork/10.),'feeInCents':power*costPerkWh_DCfast,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'EVI_Work_Level2(7.2|AC)','numStalls':np.ceil(row.numSlowChargersWork/10.),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
    output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
    output = output.loc[output['numStalls'] > 0, :]
    output['numStalls'] = output['numStalls'].astype('int')
    output.to_csv('output/taz-parking-rich10-200mi-fast-limited-l2-'+str(int(power)) +'-'+scenario+'.csv',index=False,quotechar='\'')
    print('Rich10 '+scenario)
    grouped = output.groupby('chargingType').agg('sum')*0.14
    print(grouped)
    print('----------')
    
    output = []
    
    for idx, row in taz_scenario.iterrows():
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetPaidParking),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetFreeParking),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParkingCorrected),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.residential_nocharger),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParking ),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking ),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces ),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        
        #newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetPaidParking * (work_spot_charger_fraction)),'feeInCents':row.prkcst15+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        #output.append(newrow)
        #newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetFreeParking * (work_spot_charger_fraction)),'feeInCents':0+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        #output.append(newrow)
        #newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.MadeUpWorkSpaces * (work_spot_charger_fraction)),'feeInCents':0+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        #output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'HomeLevel2(7.2|AC)','numStalls':np.ceil(row.home_l2),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'HomeLevel1(1.8|AC)','numStalls':np.ceil(row.home_l1),'feeInCents':1.8*costPerkWh_Res1,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'EVI_Public_DCFast(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.numFastChargersPublic/5.),'feeInCents':power*costPerkWh_DCfast + row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'EVI_Public_Level2(7.2|AC)','numStalls':np.ceil(row.numSlowChargersPublic/5.),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'EVI_Work_DCFast(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.numFastChargersWork/5.),'feeInCents':power*costPerkWh_DCfast,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'EVI_Work_Level2(7.2|AC)','numStalls':np.ceil(row.numSlowChargersWork/5.),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
    output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
    output = output.loc[output['numStalls'] > 0, :]
    output['numStalls'] = output['numStalls'].astype('int')
    output.to_csv('output/taz-parking-rich5-200mi-fast-limited-l2-'+str(int(power)) +'-'+scenario+'.csv',index=False,quotechar='\'')
    print('Rich '+scenario)
    grouped = output.groupby('chargingType').agg('sum')*0.14
    print(grouped)
    print('----------')
    
    output = []
    
    for idx, row in taz_scenario.iterrows():
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetPaidParking),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetFreeParking),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParkingCorrected),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.residential_nocharger),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParking ),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking ),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces ),'feeInCents':0,'ReservedFor':'Any'}
        output.append(newrow)
        
        #newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetPaidParking * (work_spot_charger_fraction)),'feeInCents':row.prkcst15+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        #output.append(newrow)
        #newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetFreeParking * (work_spot_charger_fraction)),'feeInCents':0+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        #output.append(newrow)
        #newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'WorkLevel2(7.2|AC)','numStalls':np.ceil(row.MadeUpWorkSpaces * (work_spot_charger_fraction)),'feeInCents':0+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
        #output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'HomeLevel2(7.2|AC)','numStalls':np.ceil(row.home_l2),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'HomeLevel1(1.8|AC)','numStalls':np.ceil(row.home_l1),'feeInCents':1.8*costPerkWh_Res1,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'EVI_Public_DCFast(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.numFastChargersPublic/2.),'feeInCents':power*costPerkWh_DCfast + row.prkcst15,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'EVI_Public_Level2(7.2|AC)','numStalls':np.ceil(row.numSlowChargersPublic/2.),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'EVI_Work_DCFast(' + str(int(power)) +'|DC)','numStalls':np.ceil(row.numFastChargersWork/2.),'feeInCents':power*costPerkWh_DCfast,'ReservedFor':'Any'}
        output.append(newrow)
        
        newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'EVI_Work_Level2(7.2|AC)','numStalls':np.ceil(row.numSlowChargersWork/2.),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
        output.append(newrow)
        
    output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
    output = output.loc[output['numStalls'] > 0, :]
    output['numStalls'] = output['numStalls'].astype('int')
    output.to_csv('output/taz-parking-rich2-200mi-fast-limited-l2-'+str(int(power)) +'-'+scenario+'.csv',index=False,quotechar='\'')
    print('Rich '+scenario)
    grouped = output.groupby('chargingType').agg('sum')*0.14
    print(grouped)
    print('----------')


#%%

taz_all['PredictedOffStreetPaidParkingCorrected'] = taz_all['PredictedOffStreetPaidParking'] - taz_all['ScarcePublicPlugs']
taz_all.loc[taz_all['PredictedOffStreetPaidParkingCorrected'] < 0,'PredictedOffStreetPaidParkingCorrected'] = 0

costPerkWh_DCfast = 200.
costPerkWh_Public2 = 200.
costPerkWh_Res2 = 25.
costPerkWh_Res1 = 25.


output = []

for idx, row in taz_all.iterrows():
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetPaidParking*sample),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(50.0|DC)','numStalls':np.ceil(row.ScarcePublicPlugs*sample),'feeInCents':row.prkcst15 + 50.0 * costPerkWh_DCfast,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetFreeParking*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParkingCorrected*sample),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpResidentialSpaces*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/taz-parking-sparse-50.csv',index=False,quotechar='\'')

output = []

for idx, row in taz_all.iterrows():
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetPaidParking*sample),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(150.0|DC)','numStalls':np.ceil(row.ScarcePublicPlugs*sample),'feeInCents':row.prkcst15 + 150.0 * costPerkWh_DCfast,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetFreeParking*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParkingCorrected*sample),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpResidentialSpaces*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/taz-parking-sparse-150.csv',index=False,quotechar='\'')

output = []

for idx, row in taz_all.iterrows():
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetPaidParking*sample),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(250.0|DC)','numStalls':np.ceil(row.ScarcePublicPlugs*sample),'feeInCents':row.prkcst15 + 250.0 * costPerkWh_DCfast,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetFreeParking*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParkingCorrected*sample),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpResidentialSpaces*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/taz-parking-sparse-250.csv',index=False,quotechar='\'')

output = []

for idx, row in taz_all.iterrows():
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetPaidParking*sample),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(150.0|DC)','numStalls':np.ceil(row.ScarcePublicPlugs*sample),'feeInCents':row.prkcst15 + 150.0 * costPerkWh_DCfast,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetFreeParking*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParkingCorrected*sample*0.9),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'level2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetPaidParkingCorrected*sample*0.25),'feeInCents':row.prkcst15 + 7.2 * costPerkWh_Public2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpResidentialSpaces*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'level2(7.2|AC)','numStalls':np.ceil(row.MadeUpResidentialSpaces*sample),'feeInCents':7.2 * costPerkWh_Res2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'level2(7.2|AC)','numStalls':np.ceil(row.MadeUpWorkSpaces*sample),'feeInCents':7.2 * costPerkWh_Public2,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/taz-parking-sparse-150-l2.csv',index=False,quotechar='\'')

output = []

for idx, row in taz_all.iterrows():
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetPaidParking*sample),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(150.0|DC)','numStalls':np.ceil(row.ScarcePublicPlugs*sample),'feeInCents':row.prkcst15 + 150.0 * costPerkWh_DCfast,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOnStreetFreeParking*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetPaidParkingCorrected*sample),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'level2(7.2|AC)','numStalls':np.ceil(row.PredictedOffStreetPaidParkingCorrected*sample),'feeInCents':row.prkcst15 + 7.2 * costPerkWh_Res2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.PredictedOffStreetFreeParking*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpResidentialSpaces*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Residential','pricingModel':'Block','chargingType':'level2(7.2|AC)','numStalls':np.ceil(row.MadeUpResidentialSpaces*sample),'feeInCents':7.2 * costPerkWh_Res2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.MadeUpWorkSpaces*sample),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz_id,'parkingType':'Workplace','pricingModel':'Block','chargingType':'level2(7.2|AC)','numStalls':np.ceil(row.MadeUpWorkSpaces*sample),'feeInCents':7.2 * costPerkWh_Public2,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/taz-parking-sparse-150-morel2.csv',index=False,quotechar='\'')

output = []

for idx, row in taz_all.iterrows():
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(50.0|DC)','numStalls':np.ceil(row.ScarceDepotPlugs*sample),'feeInCents':0.0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/depot-parking-sparse-50.csv',index=False,quotechar='\'')

output = []

for idx, row in taz_all.iterrows():
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(150.0|DC)','numStalls':np.ceil(row.ScarceDepotPlugs*sample),'feeInCents':0.0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/depot-parking-sparse-150.csv',index=False,quotechar='\'')

output = []

for idx, row in taz_all.iterrows():
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(250.0|DC)','numStalls':np.ceil(row.ScarceDepotPlugs*sample),'feeInCents':0.0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/depot-parking-sparse-250.csv',index=False,quotechar='\'')

output = []

for idx, row in taz_all.iterrows():
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(50.0|DC)','numStalls':10000000,'feeInCents':0.0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/depot-parking-unlimited-50.csv',index=False,quotechar='\'')

output = []

for idx, row in taz_all.iterrows():
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(150.0|DC)','numStalls':10000000,'feeInCents':0.0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/depot-parking-unlimited-150.csv',index=False,quotechar='\'')

output = []

for idx, row in taz_all.iterrows():
    newrow = {'taz':row.taz_id,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(250.0|DC)','numStalls':10000000,'feeInCents':0.0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/depot-parking-unlimited-250.csv',index=False,quotechar='\'')


#%% Do the same thing for sf-light tazs

taz_sflight = gpd.read_file('data/BEAM/taz/sf-light-tazs.shp').to_crs({'init': 'epsg:4326'})
taz_sflight['taz_id'] = taz_sflight['name']
taz_sflight = taz_sflight.set_index('name', drop=True)
onstp_with_sftaz = gpd.sjoin(taz_sflight,onstp[['prkg_sply','geometry','length']],how='inner',op='intersects')
sftaz_with_onstp = onstp_with_sftaz.groupby(onstp_with_sftaz.index).agg({'prkg_sply':'sum','length':'sum'}).fillna(0)
sftaz_with_onstp.rename(columns={'length':'length_SF','prkg_sply':'OnStreetParking'},inplace=True)

offstp_with_sftaz = gpd.sjoin(taz_sflight,offstp[['RegCap','ValetCap','MCCap','OffStreetParking','PaidPublicParking','FreePublicParking','WorkParking','geometry']],how='inner',op='intersects')
sftaz_with_offstp = offstp_with_sftaz.groupby(offstp_with_sftaz.index).agg({'taz_id':'first','OffStreetParking':'sum','PaidPublicParking':'sum','FreePublicParking':'sum','WorkParking':'sum'}).fillna(0)


meters_with_sftaz = gpd.sjoin(taz_sflight,parking_meters[['OBJECTID','geometry']],how='inner',op='intersects')
sftaz_with_meters = meters_with_sftaz.groupby(meters_with_sftaz.index).agg({'OBJECTID':'count'}).fillna(0)
sftaz_with_meters.rename(columns={'OBJECTID':'ParkingMeters'},inplace=True)


sftaz_with_baytaz = gpd.sjoin(gpd.GeoDataFrame(taz_sflight['geometry'].centroid).rename(columns={0:'geometry'}).set_geometry('geometry'), taz_all[['geometry','oprkcst15','prkcst15','taz_id','sfdu15_den','mfdu15_den','totpop15_den']], how='left').rename(columns={'geometry':'centroid'})

sftaz_all = taz_sflight.merge(sftaz_with_onstp,left_index=True,right_index=True,how='left')
sftaz_all = sftaz_all.merge(sftaz_with_offstp,left_index=True,right_index=True,how='left')
sftaz_all = sftaz_all.merge(sftaz_with_meters, left_index=True, right_index = True, how='left')
sftaz_all = sftaz_all.merge(sftaz_with_baytaz, left_index=True, right_index = True, how='left').replace(np.nan,0)
sftaz_all['inSFproper'] = sftaz_all['geometry'].apply(lambda x: sf_boundary.contains(x.centroid))
sftaz_all['area'] = sftaz_all['geometry'].to_crs({'init': 'epsg:3395'}).area/10**3 # in 1000s of sq meters (good numerically)
sftaz_all['sfdu15'] = sftaz_all['sfdu15_den'] * sftaz_all['area']
sftaz_all['mfdu15'] = sftaz_all['mfdu15_den'] * sftaz_all['area']
sftaz_all['totpop15'] = sftaz_all['totpop15_den'] * sftaz_all['area']
sftaz_all['MadeUpResidentialSpaces'] = np.ceil(sftaz_all['sfdu15'] + 0.5*sftaz_all['mfdu15'])

sftaz_all.loc[~sftaz_all['inSFproper'],['OffStreetParking','FreePublicParking','WorkParking','OnStreetParking']] = 1000000


#%% Build output file

output = []

sample=1.0
costPerkWh_DCfast = 2.
costPerkWh_Public2 = 2.
costPerkWh_Res2 = 0.25
costPerkWh_Res1 = 0.25

for idx, row in sftaz_all.iterrows(): 
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':row.ParkingMeters,'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.min([row.OnStreetParking - row.ParkingMeters,0]),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':row.PaidPublicParking,'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':row.FreePublicParking,'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':row.MadeUpResidentialSpaces,'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':row.WorkParking,'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/sf-taz-parking-base.csv',index=False)

output = []



for idx, row in sftaz_all.iterrows(): 
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'DCfast(50.0|DC)','numStalls':np.ceil(row.PaidPublicParking*sample),'feeInCents':row.prkcst15+50*costPerkWh_DCfast,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'DCfast(50.0|DC)','numStalls':np.ceil(row.FreePublicParking*sample),'feeInCents':50*costPerkWh_DCfast,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'Level2(7.2|AC)','numStalls':np.ceil(row.PaidPublicParking*sample),'feeInCents':row.prkcst15+7.2*costPerkWh_Public2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'Level2(7.2|AC)','numStalls':np.ceil(row.FreePublicParking*sample),'feeInCents':7.2*costPerkWh_Public2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Residential','pricingModel':'Block','chargingType':'Level2(7.2|AC)','numStalls':np.ceil(row.MadeUpResidentialSpaces*sample),'feeInCents':7.2*costPerkWh_Res2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Residential','pricingModel':'Block','chargingType':'Level1(1.8|AC)','numStalls':np.ceil(row.MadeUpResidentialSpaces*sample),'feeInCents':1.8*costPerkWh_Res1,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Workplace','pricingModel':'Block','chargingType':'DCfast(50.0|DC)','numStalls':np.ceil(row.WorkParking*sample),'feeInCents':50*costPerkWh_DCfast,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Workplace','pricingModel':'Block','chargingType':'Level2(7.2|AC)','numStalls':np.ceil(row.WorkParking*sample),'feeInCents':7.2*costPerkWh_Public2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Workplace','pricingModel':'Block','chargingType':'Level1(1.8|AC)','numStalls':np.ceil(row.WorkParking*sample),'feeInCents':1.8*costPerkWh_Public2,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':row.ParkingMeters,'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.min([row.OnStreetParking - row.ParkingMeters,0]),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':row.OnStreetParking,'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':row.FreePublicParking,'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Residential','pricingModel':'Block','chargingType':'NoCharger','numStalls':row.MadeUpResidentialSpaces,'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':row.WorkParking,'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/sf-taz-parking-unlimitedcharging.csv',index=False)






#%% Build output file

output = []

sf_pop = sftaz_all['totpop15'].sum()
simulated_pop = 5000
#factor = simulated_pop/sf_pop
factor = 1.0

for idx, row in sftaz_all.iterrows():
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.ParkingMeters*factor),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(np.min([row.OnStreetParking - row.ParkingMeters,0])*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'ChargingStationType2','numStalls':np.ceil(row.PaidPublicParking*factor),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.FreePublicParking*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Residential','pricingModel':'Block','chargingType':'ChargingStationType2','numStalls':np.ceil(row.MadeUpResidentialSpaces*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Workplace','pricingModel':'Block','chargingType':'ChargingStationType2','numStalls':np.ceil(row.WorkParking*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/sf-taz-parking-base-L2.csv',index=False)


output = []

sf_pop = sftaz_all['totpop15'].sum()
simulated_pop = 50000
factor = simulated_pop/sf_pop
#factor = 1.0

for idx, row in sftaz_all.iterrows():
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.ParkingMeters*factor),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(np.min([row.OnStreetParking - row.ParkingMeters,0])*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'ChargingStationType2','numStalls':np.ceil(row.PaidPublicParking*factor),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.FreePublicParking*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Residential','pricingModel':'Block','chargingType':'ChargingStationType2','numStalls':np.ceil(row.MadeUpResidentialSpaces*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Workplace','pricingModel':'Block','chargingType':'ChargingStationType2','numStalls':np.ceil(row.WorkParking*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/sf-taz-parking-base-L2-1k.csv',index=False)


output = []

sf_pop = sftaz_all['totpop15'].sum()
simulated_pop = 20000
factor = simulated_pop/sf_pop
#factor = 1.0

for idx, row in sftaz_all.iterrows():
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.ParkingMeters*factor),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(np.min([row.OnStreetParking - row.ParkingMeters,0])*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'ChargingStationType2','numStalls':np.ceil(row.PaidPublicParking*factor),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.FreePublicParking*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Residential','pricingModel':'Block','chargingType':'ChargingStationType2','numStalls':np.ceil(row.MadeUpResidentialSpaces*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Workplace','pricingModel':'Block','chargingType':'ChargingStationType2','numStalls':np.ceil(row.WorkParking*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/sf-taz-parking-base-L2-10k.csv',index=False)

output = []

sf_pop = sftaz_all['totpop15'].sum()
simulated_pop = 2500
#factor = simulated_pop/sf_pop

for idx, row in sftaz_all.iterrows():
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.ParkingMeters*factor),'feeInCents':row.oprkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(np.min([row.OnStreetParking - row.ParkingMeters,0])*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(50.0|DC)','numStalls':np.ceil(row.PaidPublicParking*factor),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.FreePublicParking*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Residential','pricingModel':'Block','chargingType':'householdsocket','numStalls':np.ceil(row.MadeUpResidentialSpaces*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Workplace','pricingModel':'Block','chargingType':'NoCharger','numStalls':np.ceil(row.WorkParking*factor),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/sf-taz-parking-rich-50.csv',index=False)

output = []

for idx, row in sftaz_all.iterrows():
    newrow = {'taz':row.taz,'parkingType':'Public','pricingModel':'Block','chargingType':'Custom(50.0|DC)','numStalls':np.ceil(row.PaidPublicParking*0.5),'feeInCents':row.prkcst15,'ReservedFor':'Any'}
    output.append(newrow)
    newrow = {'taz':row.taz,'parkingType':'Workplace','pricingModel':'Block','chargingType':'Custom(50.0|DC)','numStalls':np.ceil(row.WorkParking*0.5),'feeInCents':0,'ReservedFor':'Any'}
    output.append(newrow)
output = pd.DataFrame(output, columns = ['taz','parkingType','pricingModel','chargingType','numStalls','feeInCents','ReservedFor'])
output = output.loc[output['numStalls'] > 0, :]
output['numStalls'] = output['numStalls'].astype('int')
output.to_csv('output/sf-depot-parking-rich-50.csv',index=False)