
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

depot = pd.read_csv('data/depot-parking-rich-100-b-lt.csv')
evi = pd.read_csv('data/taz-charging-rich-b-lt.csv')
parking = pd.read_csv('data/taz-parking-no-chargers.csv')

evi_public = evi.loc[(evi['parkingType']=='Public') | (evi['parkingType']=='Workplace')]
evi_residential = evi.loc[(evi['parkingType']=='Residential')]
#%%

residential_sample = [0.1,0.5,1.0,2.0,10.0]
charging_power = [50,100,150,200,250]
depot_sample = 0.1
public_sample = 0.1

#%%
def draw_prob(n,p):
    if p <= 1:
        return np.random.binomial(n,p)
    else:
        fp = np.floor(p)
        rp = p - fp
        return fp * np.array(n) + np.random.binomial(n,rp)

#%% Build output file

for res in range(5):
    parking_out = parking.copy()
    public_out = evi_public.copy()
    public_out['numStalls'] = draw_prob(public_out['numStalls'],public_sample)
    residential_out = evi_residential.copy()
    residential_out['numStalls'] = draw_prob(residential_out['numStalls'],residential_sample[res])
    all_out = pd.concat([evi_public,evi_residential,parking]).sort_values(by='taz')
    all_out['chargingType'] = all_out['chargingType'].str.replace('50','150')
    all_out.to_csv('out/taz_parking_plugs_' + str(residential_sample[res]) + '_power_150.csv',index=False)
    
for pow in range(5):
    parking_out = parking.copy()
    public_out = evi_public.copy()
    public_out['numStalls'] = draw_prob(public_out['numStalls'],public_sample)
    residential_out = evi_residential.copy()
    residential_out['numStalls'] = draw_prob(residential_out['numStalls'],residential_sample[2])
    all_out = pd.concat([evi_public,evi_residential,parking]).sort_values(by='taz')
    all_out['chargingType'] = all_out['chargingType'].str.replace('50',str(charging_power[pow]))
    all_out.to_csv('out/taz_parking_plugs_1.0_power_' + str(int(charging_power[pow])) + '.csv',index=False)
    
    depot_out = depot.copy()
    depot_out['chargingType'] = depot_out['chargingType'].str.replace('150',str(int(charging_power[pow])))
    depot_out['numStalls'] = draw_prob(depot_out['numStalls'],depot_sample)
    depot_out.to_csv('out/depot_parking_power_' + str(int(charging_power[pow])) + '.csv',index=False)


#%%

sample=1.0
infrastructure_sample = 0.1

vehiclesWithWorkplaceCharging = 0.9*0.08

costPerkWh_DCfast = 2.
costPerkWh_Public2 = 2.
costPerkWh_Res2 = 0.25
costPerkWh_Res1 = 0.25

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