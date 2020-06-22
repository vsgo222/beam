#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Wed Jul 17 17:04:54 2019

@author: zaneedell
"""

import os
import pandas as pd
import numpy as np
from ipfn import ipfn
#%%

output = []
probs = pd.DataFrame()
ii = 0
for path, subdirs, files in os.walk("2019_08_19_beam_models/"):
    for name in files:
        full_name = os.path.join(path, name)
        if name.endswith(".csv"):
            ii += 1
            splitPath = path.split('/')
            json_loc = full_name[:-4] + '_metadata.json'
            js = pd.read_json(json_loc,lines=True)
            if 'diesel' in name:
                fuelType = 'diesel'
            else:
                fuelType = name.split('-')[0]
            newrow = {'path':path[23:],'scenario':splitPath[1].split('_')[0] + '_' + splitPath[1].split('_')[-1],'incomeGroup':splitPath[2][7:],'fileName':name,
                      'powertrain':fuelType, 'L1':'L1' in name, 'L3':'L3' in name, 'L5':'L5' in name, 
                      'primary':not ('sustaining' in name), 'income_median':js['income_median'].values[0], 
                      'fleet_perc':js['fleet_perc'].values[0],'useful_batt_kwh':js['useful_batt_kwh'].values[0]}
#            print(newrow)
            output.append(newrow)
#            js = pd.read_json(name,lines=True)
            
            
energyFiles = pd.DataFrame(output)
energyFiles['L1'] = (~energyFiles['L3'] & ~energyFiles['L5'])
energyFiles['secondaryFileName'] = None

otherVehicles = pd.read_csv('data/vehicleTypes_others.csv')
incomeGroups = pd.read_csv('data/incomeGroups.csv',index_col=0)

targetRange = 300

#%%

fleets = ['LowTech_2019','LowTech_2045','HighTech_2045','LowTech_2045','HighTech_2045','LowTech_2045']
scenarios = ['base','b','b','recharge','recharge','recharge2040']
incomeBins = ['10000_to_25000','25000_to_50000','50000_to_75000','75000_to_100000','100000_to_200000','200000_to_100000000']
powertrains = ['conv','ev','hev','phev','diesel']
automationLevels = ['L1','L3','L5']

outFileNames = ['vehicletypes-baseline-'+ str(targetRange) +'.csv',
                'vehicletypes-b-lowtech-'+ str(targetRange) +'.csv',
                'vehicletypes-b-hightech-'+ str(targetRange) +'.csv',
                'vehicletypes-recharge-lowtech-'+ str(targetRange) +'.csv',
                'vehicletypes-recharge-hightech-'+ str(targetRange) +'.csv',
                'vehicletypes-recharge2040-'+ str(targetRange) +'.csv']

automation_powertrain = pd.read_csv('data/automation-powertrain.csv',index_col=[0,1])
automation_ridehail = pd.read_csv('data/automation-ridehail.csv',index_col=[0,1])
ridehail_powertrain = pd.read_csv('data/ridehail-powertrain.csv',index_col=[0,1])


kwh2j = 3.6e+6 / 0.8

def getPrimaryFuelType(powertrain):
    if powertrain.values[0] == 'ev':
        return 'electricity'
    elif powertrain.values[0] == 'diesel':
        return 'diesel'
    elif powertrain.values[0] == 'phev':
        return 'electricity'
    else:
        return 'gasoline'

def getSecondaryFuelType(powertrain):
    if powertrain.values[0] == 'phev':
        return 'gasoline'
    else:
        return None

def getFuelConsumptionRate(powertrain):
    if powertrain.values[0] == 'ev':
        return 626.0
    elif powertrain.values[0] == 'phev':
        return 626.0
    elif powertrain.values[0] == 'conv':
        return 3655.98
    else:
        return 1300.0
    
def getSecondaryFuelConsumptionRate(powertrain):
    if powertrain.values[0] == 'phev':
        return 1300.0
    else:
        return None

def getFuelCapacity(row, shortrange):
    if row['powertrain'].values[0] == 'ev':
        return targetRange*1609.34 * 626.0
    elif row['powertrain'].values[0] == 'phev':
        return row['useful_batt_kwh'].values[0]*kwh2j
    elif row['powertrain'].values[0] ==  'conv':
        return 3655980000.0
    else:
        return 3655980000.0
    
def getSecondaryFuelCapacity(powertrain):
    if powertrain.values[0] == 'phev':
        return 3655980000.0
    else:
        return None
#incomeBins = ['10000_to_25000','25000_to_50000','50000_to_75000','75000_to_100000','100000_to_200000','200000_to_100000000']

def getSecondaryEnergyFile(row):
    if row['powertrain'].values[0] == 'phev':
        return row['path'].values[0] + '/' + row['secondaryFileName'].values[0]+'.gz'
    else:
        return None

def getSeatingCapacity(automationLevel):
    if automationLevel == 'L5':
        return 4
    else:
        return 3

def getProbabilityString(pv_prob, rh_prob, incomeBin,powertrain,automationLevel):
    real_rh_prob = rh_prob
    if incomeBin == '10000_to_25000':
        incomeStr = '0-25'
    elif incomeBin == '25000_to_50000':
        incomeStr = '25-50'
    elif incomeBin == '50000_to_75000':
        incomeStr = '50-75'
    elif incomeBin == '75000_to_100000':
        incomeStr = '75-100'
    elif incomeBin == '100000_to_200000':
        incomeStr = '100-200'
    elif incomeBin == '200000_to_100000000':
        incomeStr = '200-9999'
    return 'ridehail | all:' + str(real_rh_prob) + '; income | ' + incomeStr + ':' + str(pv_prob)


def getProbabilities(vehicles,powertrain,automationLevel,automationDistribution):
    PV_tot = automationDistribution.iloc[:3].sum()
    RH_tot = automationDistribution.iloc[3:].sum()
    
    if automationLevel == 'L1':
        sub = vehicles.loc[vehicles['L1'] & vehicles['primary'] & (vehicles['powertrain'] == powertrain),:].copy()
        RH_prob = automationDistribution[3]*sub['fleet_perc'].values/RH_tot/6.
        PV_prob = automationDistribution[0]*sub['fleet_perc'].values/PV_tot
        if powertrain == 'phev':
            sub['secondaryFileName'] = vehicles.loc[vehicles['L1'] & ~vehicles['primary'] & (vehicles['powertrain'] == powertrain),'fileName'].values
            sub['secondaryFuelType'] = 'gasoline'
    elif automationLevel == 'L3':
        sub = vehicles.loc[vehicles['L3'] & vehicles['primary'] & (vehicles['powertrain'] == powertrain),:].copy()
        RH_prob = automationDistribution[4]*sub['fleet_perc'].values/RH_tot/6.
        PV_prob = automationDistribution[1]*sub['fleet_perc'].values/PV_tot
        if powertrain == 'phev':
            sub['secondaryFileName'] = vehicles.loc[vehicles['L3'] & ~vehicles['primary'] & (vehicles['powertrain'] == powertrain),'fileName'].values
            sub['secondaryFuelType'] = 'gasoline'
    elif automationLevel == 'L5':
        sub = vehicles.loc[vehicles['L5'] & vehicles['primary'] & (vehicles['powertrain'] == powertrain),:].copy()
        RH_prob = automationDistribution[5]*sub['fleet_perc'].values/RH_tot/6.
        PV_prob = automationDistribution[2]*sub['fleet_perc'].values/PV_tot
        if powertrain == 'phev':
            sub['secondaryFileName'] = vehicles.loc[vehicles['L5'] & ~vehicles['primary'] & (vehicles['powertrain'] == powertrain),'fileName'].values
            sub['secondaryFuelType'] = 'gasoline'
    out = dict()
    out['row'] = sub
    out['RH_prob'] = RH_prob
    out['PV_prob'] = PV_prob
    return out

def getVehicleInfo(vehicles,powertrain,automationLevel):
    if automationLevel == 'L1':
        sub = vehicles.loc[vehicles['L1'] & vehicles['primary'] & (vehicles['powertrain'] == powertrain),:].copy()
        if powertrain == 'phev':
            sub['secondaryFileName'] = vehicles.loc[vehicles['L1'] & ~vehicles['primary'] & (vehicles['powertrain'] == powertrain),'fileName'].values
    elif automationLevel == 'L3':
        sub = vehicles.loc[vehicles['L3'] & vehicles['primary'] & (vehicles['powertrain'] == powertrain),:].copy()
        if powertrain == 'phev':
            sub['secondaryFileName'] = vehicles.loc[vehicles['L3'] & ~vehicles['primary'] & (vehicles['powertrain'] == powertrain),'fileName'].values
    elif automationLevel == 'L5':
        sub = vehicles.loc[vehicles['L5'] & vehicles['primary'] & (vehicles['powertrain'] == powertrain),:].copy()
        if powertrain == 'phev':
            sub['secondaryFileName'] = vehicles.loc[vehicles['L5'] & ~vehicles['primary'] & (vehicles['powertrain'] == powertrain),'fileName'].values
    return sub


def getVehicleTypeId(row, automationLevel):
    return row['powertrain'].values[0] + '-' + automationLevel + '-' + row['incomeGroup'].values[0].replace('_','-') +'-'+ row['scenario'].values[0].replace('_','-')

def getLine(row, automationLevel, RH_prob, PV_prob):
    line = {'vehicleTypeId': getVehicleTypeId(row, automationLevel), 'seatingCapacity':getSeatingCapacity(automationLevel), 'standingRoomCapacity':0,
       'lengthInMeter':4.5, 'primaryFuelType':getPrimaryFuelType(row['powertrain']),
       'primaryFuelConsumptionInJoulePerMeter':getFuelConsumptionRate(row['powertrain']), 'primaryFuelCapacityInJoule':getFuelCapacity(row, False),
       'primaryVehicleEnergyFile':row['path'].values[0] + '/' + row['fileName'].values[0]+'.gz', 'secondaryFuelType':getSecondaryFuelType(row['powertrain']),
       'secondaryFuelConsumptionInJoulePerMeter':getSecondaryFuelConsumptionRate(row['powertrain']), 'secondaryVehicleEnergyFile':getSecondaryEnergyFile(row),
       'secondaryFuelCapacityInJoule':getSecondaryFuelCapacity(row['powertrain']), 'automationLevel':automationLevel[1], 'maxVelocity':None,
       'passengerCarUnit':None, 'rechargeLevel2RateLimitInWatts':None,
       'rechargeLevel3RateLimitInWatts':None, 'vehicleCategory':'Car',
       'sampleProbabilityWithinCategory':PV_prob, 'sampleProbabilityString':getProbabilityString(PV_prob, RH_prob, row['incomeGroup'].values[0],row['powertrain'].values[0],automationLevel)}
    return line
    
all_output = []
idx = 0

runtype = 'gemini'

for fleet in fleets:
    output = []
    sub = energyFiles.loc[(energyFiles['scenario'] == fleet),:]
    #powertrain_income = sub.loc[sub['L1'],'fleet_perc'].values
    powertrain_income = pd.DataFrame(columns=powertrains, index=incomeBins)
    income_automation = pd.DataFrame(columns=automationLevels, index=incomeBins)
    powertrain_ridehail = pd.DataFrame(columns=automationLevels, index=incomeBins)
    
    
    a_r = automation_ridehail.iloc[:,idx].unstack().transpose()
    a_p = automation_powertrain.iloc[:,idx].unstack()
    r_p = ridehail_powertrain.iloc[:,idx].unstack()

    
    portion_noncav_L1 = a_r.sum(axis=1)['L1']/(a_r.sum(axis=1)['L1'] + a_r.sum(axis=1)['L3'])
    
    total_number_cavs = a_r.sum(axis=1)[2]
    number_cavs_shifted = incomeGroups.iloc[:3].sum().values[0]*total_number_cavs
    
    for index, incomeBin in enumerate(incomeBins):
        
        vehicles = sub.loc[sub['incomeGroup'] == incomeBin,:]
        income_total = vehicles.drop_duplicates('powertrain')['fleet_perc'].sum()
        vehicles['fleet_perc'] = vehicles['fleet_perc']/income_total

        powertrain_perc = vehicles.drop_duplicates('powertrain')[['fleet_perc','powertrain']].set_index('powertrain')
        powertrain_income.loc[incomeBin,:] = powertrain_perc.loc[powertrains].values[:,0]*incomeGroups.loc[incomeBin].values[0]
        income_automation.loc[incomeBin,:] = a_r.sum(axis=1)*incomeGroups.loc[incomeBin].values[0]
    
    number_noncavs_to_decrease = income_automation.iloc[3:,:2].sum().sum()

    income_automation.iloc[:3,0] += income_automation.iloc[:3,2].values*portion_noncav_L1
    income_automation.iloc[:3,1] += income_automation.iloc[:3,2].values*(1.0-portion_noncav_L1)
    income_automation.iloc[:3,2] = 0
    income_automation.iloc[3:,:2] *= (number_noncavs_to_decrease - number_cavs_shifted)/number_noncavs_to_decrease
    income_automation.iloc[:,2] += incomeGroups['percentage'] - income_automation.sum(axis=1)

        
    totalFleet = powertrain_income.sum(axis=0)
    
    diesel_frac = totalFleet['diesel']/totalFleet['conv']
    a_p.loc['diesel',:] = diesel_frac*a_p.loc['conv',:]
    a_p.loc['conv',:] = (1.0-diesel_frac)*a_p.loc['conv',:]
    
    r_p.loc['diesel',:] = diesel_frac*r_p.loc['conv',:]
    r_p.loc['conv',:] = (1.0-diesel_frac)*r_p.loc['conv',:]

    percs = np.zeros((6,5,3,2))+(1./180.)
    if runtype == 'gemini':
        IPF = ipfn.ipfn(percs, [powertrain_income.values,a_p.values,r_p.values,a_r.values,income_automation.values],[[0,1],[1,2],[1,3],[2,3],[0,2]])
    else:
        IPF = ipfn.ipfn(percs, [powertrain_income.values,a_p.values,a_r.values,income_automation.values],[[0,1],[1,2],[2,3],[0,2]])
    fit = IPF.iteration()
    #PV = pd.Panel((fit[:,:,:,0]),items=incomeGroups.index,major_axis = a_p.index, minor_axis = a_p.columns).to_frame()
    PV = pd.DataFrame(
    data=fit[:,:,:,0].flatten(),
    index=pd.MultiIndex.from_product([incomeGroups.index,a_p.index, a_p.columns])
)[0].unstack(level=0)
    #RH = pd.Panel((fit[:,:,:,1]),items=incomeGroups.index,major_axis = a_p.index, minor_axis = a_p.columns).to_frame()
    RH = pd.DataFrame(
    data=fit[:,:,:,1].flatten(),
    index=pd.MultiIndex.from_product([incomeGroups.index,a_p.index, a_p.columns])
)[0].unstack(level=0)
    print('BOTH')
    print((PV+RH).sum(axis=1).unstack())
    print('RH')
    print((RH).sum(axis=1).unstack()/RH.sum().sum())
    print('_____')
    for incomeBin in incomeBins:
#        a_r = automation_ridehail.iloc[:,idx].unstack().transpose()
#        a_p = automation_powertrain.iloc[:,idx].unstack()
        vehicles = sub.loc[sub['incomeGroup'] == incomeBin,:]
#        income_total = vehicles.drop_duplicates('powertrain')['fleet_perc'].sum()
#        vehicles['fleet_perc'] /= income_total
#        print(automationDistribution.iloc[:,idx])
        
#        powertrain_perc = vehicles.drop_duplicates('powertrain')[['fleet_perc','powertrain']].set_index('powertrain')
#        diesel_frac = powertrain_perc.loc['diesel'].values[0]/powertrain_perc.loc['conv'].values[0]
#        a_p.loc['diesel',:] = diesel_frac*a_p.loc['conv',:]
#        a_p.loc['conv',:] = (1.0-diesel_frac)*a_p.loc['conv',:]
        
#        percs = np.zeros((5,3,2))+(1./30.)
#        IPF = ipfn.ipfn(percs, [powertrain_perc.loc[a_p.index].values,a_p.sum(axis=0).values,a_p.values,a_r.values],[[0],[1],[0,1],[1,2]])
        #IPF = ipfn.ipfn(percs, [a_p.values,a_r.values],[[0,1],[1,2]])
#        fit = IPF.iteration()
#        probs = pd.Panel(fit, items = a_p.index, major_axis = a_p.columns, minor_axis = a_r.columns).to_frame()
#        print(probs.sum(level=0))
        for powertrain in powertrains:
            for automationLevel in automationLevels:
                #RH_prob = probs.loc[(automationLevel,'RH'),powertrain]
                #PV_prob = probs.loc[(automationLevel,'PV'),powertrain]
                RH_prob = RH.loc[(powertrain,automationLevel),incomeBin]/RH.sum().sum()
                PV_prob = PV.loc[(powertrain,automationLevel),incomeBin]/PV.sum()[incomeBin]
                

                if RH_prob + PV_prob > 0:
                    row = getVehicleInfo(vehicles,powertrain,automationLevel)
                    line = getLine(row,automationLevel,RH_prob,PV_prob)
                    output.append(line)
                    all_output.append(line)
    output = pd.DataFrame(output, columns = ['vehicleTypeId', 'seatingCapacity', 'standingRoomCapacity',
       'lengthInMeter', 'primaryFuelType',
       'primaryFuelConsumptionInJoulePerMeter', 'primaryFuelCapacityInJoule',
       'primaryVehicleEnergyFile', 'secondaryFuelType',
       'secondaryFuelConsumptionInJoulePerMeter', 'secondaryVehicleEnergyFile',
       'secondaryFuelCapacityInJoule', 'automationLevel', 'maxVelocity',
       'passengerCarUnit', 'rechargeLevel2RateLimitInWatts', \
       'rechargeLevel3RateLimitInWatts', 'vehicleCategory',
       'sampleProbabilityWithinCategory', 'sampleProbabilityString'])
    final = output.append(otherVehicles)
    final.to_csv('out/'+runtype+'_'+outFileNames[idx], index=False)
    idx += 1
all_output = pd.DataFrame(all_output, columns = ['vehicleTypeId', 'seatingCapacity', 'standingRoomCapacity',
       'lengthInMeter', 'primaryFuelType',
       'primaryFuelConsumptionInJoulePerMeter', 'primaryFuelCapacityInJoule',
       'primaryVehicleEnergyFile', 'secondaryFuelType',
       'secondaryFuelConsumptionInJoulePerMeter', 'secondaryVehicleEnergyFile',
       'secondaryFuelCapacityInJoule', 'automationLevel', 'maxVelocity',
       'passengerCarUnit', 'rechargeLevel2RateLimitInWatts', \
       'rechargeLevel3RateLimitInWatts', 'vehicleCategory',
       'sampleProbabilityWithinCategory', 'sampleProbabilityString'])
final = all_output.append(otherVehicles)
final.to_csv('out/vehicletypes-all.csv', index=False)
        #print(incomeBin)
#        print(subsub['fleet_perc'].sum())
#        for powertrain in powertrains:
#            for automationLevel in automationLevels:
#                out = getRow(energyFiles, fleet, scenario, incomeBin, powertrain, automationLevel, False)
#                if np.size(out) > 0:
#                    output.append(out)
#                    all_output.append(out)
#    out = getRow(energyFiles, fleet, scenario, 'inc133', 'ev', 'L5', True)
#    if np.size(out) > 0:
#       output.append(out)
#    output = pd.DataFrame(output, columns = ['vehicleTypeId', 'seatingCapacity', 'standingRoomCapacity',
#       'lengthInMeter', 'primaryFuelType',
#       'primaryFuelConsumptionInJoulePerMeter', 'primaryFuelCapacityInJoule',
#       'primaryVehicleEnergyFile', 'secondaryFuelType',
#       'secondaryFuelConsumptionInJoulePerMeter', 'secondaryVehicleEnergyFile',
#       'secondaryFuelCapacityInJoule', 'automationLevel', 'maxVelocity',
#       'passengerCarUnit', 'rechargeLevel2RateLimitInWatts', \
#       'rechargeLevel3RateLimitInWatts', 'vehicleCategory',
#       'sampleProbabilityWithinCategory', 'sampleProbabilityString'])
#    print(output['sampleProbabilityWithinCategory'].sum())
#    final = output.append(otherVehicles)
#    final.to_csv(outFileNames[idx], index=False)
#    idx += 1
#out = getRow(energyFiles, fleet, scenario, 'inc133', 'ev', 'L5', True)
#if np.size(out) > 0:
#    all_output.append(out)
#all_output = pd.DataFrame(all_output, columns = ['vehicleTypeId', 'seatingCapacity', 'standingRoomCapacity',
#       'lengthInMeter', 'primaryFuelType',
#       'primaryFuelConsumptionInJoulePerMeter', 'primaryFuelCapacityInJoule',
#       'primaryVehicleEnergyFile', 'secondaryFuelType',
#       'secondaryFuelConsumptionInJoulePerMeter', 'secondaryVehicleEnergyFile',
#       'secondaryFuelCapacityInJoule', 'automationLevel', 'maxVelocity',
#       'passengerCarUnit', 'rechargeLevel2RateLimitInWatts',
#       'rechargeLevel3RateLimitInWatts', 'vehicleCategory',
#       'sampleProbabilityWithinCategory', 'sampleProbabilityString'])
#final = all_output.append(otherVehicles)
#final.to_csv('vehicletypes-all.csv', index=False)
#%%            

 



#['inc17','inc36','inc61','inc86','inc133','inc327']

    
def getRow(df, fleet, scenario, incomeBin, powertrain, automationLevel,additional):
    rows = ((df['scenario'] == scenario) & (df['incomeGroup'] == incomeBin) & (df['powertrain'] == powertrain) & (df[automationLevel]))
    prob = fleet.loc[(fleet['powertrain'] == powertrain) & (fleet['automationLevel'] == automationLevel), scenario + '-Personal'].values
    rh_prob = fleet.loc[(fleet['powertrain'] == powertrain) & (fleet['automationLevel'] == automationLevel), scenario + '-RH'].values
    rh_prob = rh_prob * (incomeBin == 'inc133')
    if rows.size == 0:
        print('OH NO')
    if (np.sum(rows) == 0) & (prob + rh_prob > 0):
        rows = ((df['scenario'] == scenario) & (df['incomeGroup'] == incomeBin) & (df['powertrain'] == powertrain))
        if (np.sum(rows) == 0) & (prob > 0):
            print(scenario, incomeBin, powertrain, automationLevel)
            return []
    if prob + rh_prob > 0:
        if powertrain == 'phev':
            secondaryrows = rows & ~df['primary']
            rows = rows & df['primary']
        if np.sum(rows) > 1:
            rows = rows & ~df['L3'] & ~df['L5']
        if np.sum(rows) > 1:
            print(scenario, incomeBin, powertrain, automationLevel)
            print(df.loc[rows,:])
        elif np.sum(rows) == 0:
            print('BAD')
            print(scenario, incomeBin, powertrain, automationLevel)
        if rows.size == 0:
            print('OH NO')
        if additional:
            row = {'vehicleTypeId':powertrain+'-'+automationLevel+'-'+incomeBin+'-'+ scenario + '-shortrange-rh', 'seatingCapacity':4, 'standingRoomCapacity':0,
       'lengthInMeter':4.5, 'primaryFuelType':getPrimaryFuelType(powertrain),
       'primaryFuelConsumptionInJoulePerMeter':getFuelConsumptionRate(powertrain), 'primaryFuelCapacityInJoule':getFuelCapacity(powertrain,'L1'),
       'primaryVehicleEnergyFile':df.loc[rows,'path'].values[0] + '/' + df.loc[rows,'fileName'].values[0], 'secondaryFuelType':None,
       'secondaryFuelConsumptionInJoulePerMeter':None, 'secondaryVehicleEnergyFile':None,
       'secondaryFuelCapacityInJoule':None, 'automationLevel':automationLevel[1], 'maxVelocity':None,
       'passengerCarUnit':None, 'rechargeLevel2RateLimitInWatts':None,
       'rechargeLevel3RateLimitInWatts':None, 'vehicleCategory':'Car',
       'sampleProbabilityWithinCategory':0.0, 'sampleProbabilityString':'ridehail | all:' + str(rh_prob[0])}
            print('BLAH')
            print(rh_prob[0])
        else:
            if powertrain == 'phev':
                row = {'vehicleTypeId':powertrain+'-'+automationLevel+'-'+incomeBin+'-'+ scenario, 'seatingCapacity':4, 'standingRoomCapacity':0,
       'lengthInMeter':4.5, 'primaryFuelType':getPrimaryFuelType(powertrain),
       'primaryFuelConsumptionInJoulePerMeter':getFuelConsumptionRate(powertrain), 'primaryFuelCapacityInJoule':getFuelCapacity(powertrain,automationLevel),
       'primaryVehicleEnergyFile':df.loc[rows,'path'].values[0] + '/' + df.loc[rows,'fileName'].values[0], 'secondaryFuelType':'gasoline',
       'secondaryFuelConsumptionInJoulePerMeter':getFuelConsumptionRate('hev'), 'secondaryVehicleEnergyFile':df.loc[secondaryrows,'path'].values[0] + '/' + df.loc[secondaryrows,'fileName'].values[0],
       'secondaryFuelCapacityInJoule':getFuelCapacity('hev',automationLevel), 'automationLevel':automationLevel[1], 'maxVelocity':None,
       'passengerCarUnit':None, 'rechargeLevel2RateLimitInWatts':None,
       'rechargeLevel3RateLimitInWatts':None, 'vehicleCategory':'Car',
       'sampleProbabilityWithinCategory':prob[0]/6., 'sampleProbabilityString':getProbabilityString(prob[0], rh_prob[0], incomeBin,powertrain,automationLevel)}
            else:
                row = {'vehicleTypeId':powertrain+'-'+automationLevel+'-'+incomeBin+'-'+ scenario, 'seatingCapacity':4, 'standingRoomCapacity':0,
       'lengthInMeter':4.5, 'primaryFuelType':getPrimaryFuelType(powertrain),
       'primaryFuelConsumptionInJoulePerMeter':getFuelConsumptionRate(powertrain), 'primaryFuelCapacityInJoule':getFuelCapacity(powertrain,automationLevel),
       'primaryVehicleEnergyFile':df.loc[rows,'path'].values[0] + '/' + df.loc[rows,'fileName'].values[0], 'secondaryFuelType':None,
       'secondaryFuelConsumptionInJoulePerMeter':None, 'secondaryVehicleEnergyFile':None,
       'secondaryFuelCapacityInJoule':None, 'automationLevel':automationLevel[1], 'maxVelocity':None,
       'passengerCarUnit':None, 'rechargeLevel2RateLimitInWatts':None,
       'rechargeLevel3RateLimitInWatts':None, 'vehicleCategory':'Car',
       'sampleProbabilityWithinCategory':prob[0]/6., 'sampleProbabilityString':getProbabilityString(prob[0], rh_prob[0], incomeBin,powertrain,automationLevel)}
        return row
    if rows.size == 0:
        print('OH NO')
    return []