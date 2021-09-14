package beam.analysis.tscore;

import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionsBase;

public class CLIOptions extends OptionsBase {

    @Option(
            name = "inputDir",
            abbrev = 'i',
            help = "Directory of input files (BEAM output files)",
            defaultValue = ""
    )
    public String inputDir;

    @Option(
            name = "outputFile",
            abbrev = 'o',
            help = "Name of output text file",
            defaultValue = "WAV_Stats.txt"
    )
    public String outputFile;

    @Option(
            name = "rideHailFleetFile",
            abbrev = 'r',
            help = "Path of ridehail fleet file",
            defaultValue = ""
    )
    public String rideHailFleetFile;

}
