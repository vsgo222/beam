

def convert(seconds):
    import time
    return time.strftime("%H:%M:%S", time.gmtime(seconds))

def createUrl(foldername):
    return "https://beam-outputs.s3.amazonaws.com/output/sfbay/"+foldername

def createSetup(name,expansion_factor,percapita_factor,plot_size,settings):
    plt_setup_smart={
        'name': name,
        'expansion_factor':expansion_factor,
        'percapita_factor':percapita_factor,
        'scenarios_itr': [],
        'scenarios_id':[],
        'scenarios_year':[],
        'plot_size': plot_size,
        'bottom_labels': [],
        'top_labels': [],
        'plots_folder': "makeplots3"
    }
    plt_setup_smart['name']=name
    plt_setup_smart['expansion_factor']=expansion_factor
    plt_setup_smart['plot_size']=plot_size

    plt_setup_smart['scenarios_year']=[]
    plt_setup_smart['scenarios_id']=[]
    plt_setup_smart['scenarios_itr']=[]
    plt_setup_smart['top_labels']=[]

    for (scenarios_year,scenarios_id,scenarios_itr,bottom_label,top_label) in settings:
        plt_setup_smart['scenarios_year'].append(scenarios_year)
        plt_setup_smart['scenarios_id'].append(scenarios_id)
        plt_setup_smart['scenarios_itr'].append(scenarios_itr)
        plt_setup_smart['top_labels'].append(top_label)
        plt_setup_smart['bottom_labels'].append(bottom_label)

    return plt_setup_smart

def createSettingRow(scenarios_year,scenarios_id,scenarios_itr,bottom_label,top_label):
    return (scenarios_year,scenarios_id,scenarios_itr,bottom_label,top_label)
