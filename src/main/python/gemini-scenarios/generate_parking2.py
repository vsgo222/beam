
import pandas as pd
import numpy as np
#%% Load TAZ data from MTC

def draw_prob(n,p):
    if p <= 1:
        return np.random.binomial(n,p)
    else:
        fp = np.floor(p)
        rp = p - fp
        return fp * np.array(n) + np.random.binomial(n,rp)

def update_prices(df):
    df.loc[df['chargingType'].str.contains('1.8'), 'feeInCents'] = 50
    df.loc[df['chargingType'].str.contains('7.2'), 'feeInCents'] = 200
    df.loc[df['chargingType'].str.contains('50'), 'feeInCents'] = 2500
    df.loc[df['chargingType'].str.contains('150'), 'feeInCents'] = 7500
    df.loc[df['chargingType'].str.contains('250'), 'feeInCents'] = 12500
    return df

depot = pd.read_csv('data/depot-parking-rich-100-b-lt.csv')
evi = pd.read_csv('data/taz-charging-rich-b-lt.csv')
parking = pd.read_csv('data/taz-parking-no-chargers.csv')
evi_public = evi.loc[(evi['parkingType']=='Public') | (evi['parkingType']=='Workplace')]
evi_residential = evi.loc[(evi['parkingType']=='Residential')]
#

depot_sample = 0.1
public_sample = 0.1
residential_sample = 1.0/1.595649

charging_power_baseline_names = '150kw_250kw'
charging_power_baseline = {150: 0.9, 250: 0.1}
charging_power_low_high_names = ['50kw', '250kw']
charging_power_low_high = [{50: 1.0}, {250: 1.0}]
residential_share = [0.6, 0.3, 0.9]


public_sampled = evi_public.copy()
public_sampled['numStalls'] = draw_prob(public_sampled['numStalls'], public_sample)
parking_infinite = parking.copy()
parking_infinite['numStalls'] = 999999
public_all = pd.concat([public_sampled, parking_infinite]).sort_values(by='taz')

residential_sampled = evi_residential.copy()
residential_sampled['numStalls'] = draw_prob(residential_sampled['numStalls'], residential_sample)

depot_sampled = depot.copy()
depot_sampled['numStalls'] = draw_prob(depot_sampled['numStalls'], depot_sample)

for res_prob in residential_share:
    public_all = public_all.copy()
    residential_all = residential_sampled.copy()
    residential_all['numStalls'] = np.ceil(residential_sampled['numStalls'] * res_prob).astype(int)
    charger_all = pd.concat([public_all, residential_all]).sort_values(by='taz')
    dcfc_all = charger_all.loc[charger_all['chargingType'].str.contains('DC')]
    charger_all_out = [charger_all.loc[charger_all['chargingType'].str.contains('DC')==False]]
    depot_out = []
    all_ref = dcfc_all.copy()
    depot_ref = depot_sampled.copy()
    for power, prob in zip(charging_power_baseline.keys(), charging_power_baseline.values()):
        # pev
        all_temp = dcfc_all.copy()
        all_temp['chargingType'] = dcfc_all['chargingType'].str.replace('50', str(int(power)))
        all_temp['numStalls'] = (np.floor(dcfc_all['numStalls'] * prob)).astype(int)
        all_ref['numStalls'] = all_ref['numStalls'] - all_temp['numStalls']
        charger_all_out.append(all_temp)
        # ridehail
        depot_temp = depot_sampled.copy()
        depot_temp['chargingType'] = depot_temp['chargingType'].str.replace('150', str(int(power)))
        depot_temp['numStalls'] = (np.floor(depot_temp['numStalls'] * prob)).astype(int)
        depot_ref['numStalls'] = depot_ref['numStalls'] - depot_temp['numStalls']
        depot_out.append(depot_temp)
    # pev
    charger_all_out[len(charger_all_out)-1]['numStalls'] = charger_all_out[len(charger_all_out)-1]['numStalls'] + all_ref['numStalls']
    charger_all_out = pd.concat(charger_all_out).sort_values(by='taz')
    charger_all_out = update_prices(charger_all_out)
    charger_all_out.loc[charger_all_out.numStalls > 0].to_csv('out/gemini_taz_parking_plugs_' + str(res_prob) + '_power_' + charging_power_baseline_names + '.csv', index=False)
    # ridehail
    depot_out[len(depot_out)-1]['numStalls'] = depot_out[len(depot_out)-1]['numStalls'] + depot_ref['numStalls']
    depot_out = pd.concat(depot_out).sort_values(by='taz')
    depot_out.loc[depot_out.numStalls > 0].to_csv('out/gemini_depot_parking_power_' + charging_power_baseline_names + '.csv',index=False)


for ind in range(np.size(charging_power_low_high)):
    public_all = public_all.copy()
    residential_all = residential_sampled.copy()
    residential_all['numStalls'] = np.ceil(residential_sampled['numStalls'] * residential_share[0]).astype(int)
    charger_all = pd.concat([public_all, residential_all]).sort_values(by='taz')
    dcfc_all = charger_all.loc[charger_all['chargingType'].str.contains('DC')]
    charger_all_out = [charger_all.loc[charger_all['chargingType'].str.contains('DC')==False]]
    val = charging_power_low_high[ind]
    powers = val.keys()
    probs = val.values()
    depot_out = []
    all_ref = dcfc_all.copy()
    depot_ref = depot_sampled.copy()
    for power, prob in zip(powers, probs):
        # pev
        all_temp = dcfc_all.copy()
        all_temp['chargingType'] = dcfc_all['chargingType'].str.replace('50', str(int(power)))
        all_temp['numStalls'] = (np.floor(dcfc_all['numStalls'] * prob)).astype(int)
        all_ref['numStalls'] = all_ref['numStalls'] - all_temp['numStalls']
        charger_all_out.append(all_temp)
        # ridehail
        depot_temp = depot_sampled.copy()
        depot_temp['chargingType'] = depot_temp['chargingType'].str.replace('150', str(int(power)))
        depot_temp['numStalls'] = (np.floor(depot_temp['numStalls'] * prob)).astype(int)
        depot_ref['numStalls'] = depot_ref['numStalls'] - depot_temp['numStalls']
        depot_out.append(depot_temp)
    # pev
    charger_all_out[len(charger_all_out)-1]['numStalls'] = charger_all_out[len(charger_all_out)-1]['numStalls'] + all_ref['numStalls']
    charger_all_out = pd.concat(charger_all_out).sort_values(by='taz')
    charger_all_out = update_prices(charger_all_out)
    charger_all_out.loc[charger_all_out.numStalls > 0].to_csv('out/gemini_taz_parking_plugs_' + str(residential_share[0]) + '_power_' + str(charging_power_low_high_names[ind]) + '.csv', index=False)
    # ridehail
    depot_out[len(depot_out)-1]['numStalls'] = depot_out[len(depot_out)-1]['numStalls'] + depot_ref['numStalls']
    depot_out = pd.concat(depot_out).sort_values(by='taz')
    depot_out.loc[depot_out.numStalls > 0].to_csv('out/gemini_depot_parking_power_' + str(charging_power_low_high_names[ind]) + '.csv',index=False)




#
# for ind in range(np.size(charging_power)):
#     parking_out = parking.copy()
#     public_out = evi_public.copy()
#     public_out['numStalls'] = draw_prob(public_out['numStalls'],public_sample)
#     residential_out = evi_residential.copy()
#     residential_out['numStalls'] = draw_prob(residential_out['numStalls'],residential_sample[0]/evs_baseline)
#
#     all_out = pd.concat([public_out,residential_out,parking_out]).sort_values(by='taz')
#     all_out['chargingType'] = all_out['chargingType'].str.replace('50',str(charging_power[ind]))
#     all_out.loc[all_out['chargingType'].str.contains('1.8'), 'feeInCents'] = 50
#     all_out.loc[all_out['chargingType'].str.contains('7.2'), 'feeInCents'] = 200
#     all_out.loc[all_out['chargingType'].str.contains('50'), 'feeInCents'] = 2500
#     all_out.loc[all_out['chargingType'].str.contains('150'), 'feeInCents'] = 7500
#     all_out.loc[all_out['chargingType'].str.contains('250'), 'feeInCents'] = 12500
#
#     all_out.loc[all_out['chargingType'].str.contains('NoCharger'), 'numStalls'] = 999999
#
#     all_out.to_csv('out/gemini_taz_parking_plugs_' + str(residential_sample[0]) + '_power_' + str(int(charging_power[ind])) + '.csv',index=False)
#
#
#
#
# for ind in range(np.size(charging_power)):
#     val = charging_power[ind]
#
#     if isinstance(val,dict):
#         powers = val.keys()
#         probs = val.values()
#     else:
#         powers = [val]
#         probs = [1.0]
#
#     depot_out = []
#     for power, prob in zip(powers, probs):
#         depot_temp = depot.copy()
#         depot_temp['chargingType'] = depot_temp['chargingType'].str.replace('150',str(int(power)))
#         depot_temp['numStalls'] = draw_prob(depot_temp['numStalls'],depot_sample * prob)
#         depot_out.append(depot_temp)
#     depot_out = pd.concat(depot_out).sort_values(by='taz')
#     depot_out = depot_out.loc[depot_out.numStalls > 0]
#     depot_out.to_csv('out/gemini_depot_parking_power_' + str(scenario_names[ind]) + '.csv',index=False)
