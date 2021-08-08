package beam.analysis.plots.modality;

import beam.analysis.plots.GraphUtils;
import beam.analysis.plots.GraphsStatsAgentSimEventsListener;
import beam.sim.population.PopulationAdjustment;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.data.category.CategoryDataset;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class TourPurposeStats {
    private final Logger log = LoggerFactory.getLogger(TourPurposeStats.class);

    private final String graphTile;
    private final String xAxisTitle;
    private final String yAxisTitle;
    private final String fileName;
    private final String attributeName;
    private final Set<String> className;
    private final Map<Integer, Map<String, Double>> iterationVsPurposeClassCount;

    public TourPurposeStats() {
        className = new TreeSet<>();
        iterationVsPurposeClassCount = new HashMap<>();
        graphTile = "Tour Purpose";
        xAxisTitle = "Iteration";
        yAxisTitle = "Number of Agents";
        fileName = "tour-purposes.png";
        attributeName = "tour-purpose";
    }

    public void buildTourPurposeGraph(OutputDirectoryHierarchy ioController) {
        try {
            buildGraphFromPopulationProcessDataSet(ioController);
        } catch (Exception e) {
            log.error("exception occurred due to ", e);
        }
    }

    public void processData(Population population, IterationEndsEvent event) {
        processPopulationPlan(population, event);
    }


    private void processPopulationPlan(Population population, IterationEndsEvent event) {
        if (population == null) {
            return;
        }

        Set entries = population.getPersons().keySet();
        for (Object entry : entries) {
            String key = entry.toString();
            //for some reason the population value that gets passed into this class is not updated.
            // As a result, we take the updated population modality styles form the PopulationAdjustment
            // class. One day I hope to just update the population value in BeamSeam#L351
            String[] purposes= PopulationAdjustment.getTourPurposes(key);
            for (String purpose: purposes){
                className.add(purpose);
                Map<String, Double> purposeData;
                purposeData = iterationVsPurposeClassCount.get(event.getIteration());
                if (purposeData == null) {
                    purposeData = new HashMap<>();
                    purposeData.put(purpose, 1.0);
                    iterationVsPurposeClassCount.put(event.getIteration(), purposeData);
                } else {
                    Double purposeClassCount = purposeData.get(purpose);
                    if (purposeClassCount == null)
                        purposeClassCount = 0.0;
                    purposeClassCount = purposeClassCount + 1;
                    purposeData.put(purpose, purposeClassCount);
                    iterationVsPurposeClassCount.put(event.getIteration(), purposeData);
                }
            }
        }
    }

    private double[][] buildPurposeDataSet() {
        List<Integer> iterationCount = GraphsStatsAgentSimEventsListener.getSortedIntegerList(iterationVsPurposeClassCount.keySet());
        List<String> classList = GraphsStatsAgentSimEventsListener.getSortedStringList(className);
        if (iterationCount.size() == 0 || classList.size() == 0) {
            return null;
        }
        double[][] dataSet = new double[classList.size()][iterationCount.size()];
        for (int i = 0; i < classList.size(); i++) {
            double[] data = new double[iterationCount.size()];
            String className = classList.get(i);
            for (int j = 0; j < iterationCount.size(); j++) {
                Map<String, Double> purposeData = iterationVsPurposeClassCount.get(j);
                data[j] = purposeData.getOrDefault(className, 0D);
            }
            dataSet[i] = data;
        }
        return dataSet;
    }

    private CategoryDataset buildPurposeGraphDataSet() {
        double[][] dataSet = buildPurposeDataSet();
        if (dataSet == null) {
            return null;
        }
        return GraphUtils.createCategoryDataset("", "", dataSet);
    }

    private void buildGraphFromPopulationProcessDataSet(OutputDirectoryHierarchy ioController) throws IOException {
        CategoryDataset categoryDataset = buildPurposeGraphDataSet();
        if (categoryDataset == null) {
            return;
        }
        List<String> classList = GraphsStatsAgentSimEventsListener.getSortedStringList(className);
        final JFreeChart chart = GraphUtils.createStackedBarChartWithDefaultSettings(categoryDataset, graphTile, xAxisTitle, yAxisTitle, true);
        CategoryPlot plot = chart.getCategoryPlot();
        GraphUtils.plotLegendItems(plot, classList, categoryDataset.getRowCount());
        String graphImageFile = ioController.getOutputFilename(fileName);
        GraphUtils.saveJFreeChartAsPNG(chart, graphImageFile, GraphsStatsAgentSimEventsListener.GRAPH_WIDTH, GraphsStatsAgentSimEventsListener.GRAPH_HEIGHT);
    }
}
