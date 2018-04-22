package com.alirabiee.apistats.api.statistics;

import com.alirabiee.apistats.data.statistics.Statistics;
import com.alirabiee.apistats.data.statistics.StatisticsReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Reports the latest available transaction statistics
 */
@Controller
@RequestMapping("/statistics")
public class StatisticsController {
    private final Statistics statistics;

    @Autowired
    public StatisticsController(Statistics statistics) {
        this.statistics = statistics;
    }

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public StatisticsReport getStatistics(){
        return statistics.getReport();
    }
}
