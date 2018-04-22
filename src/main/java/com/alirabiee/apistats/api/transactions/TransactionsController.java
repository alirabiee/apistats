package com.alirabiee.apistats.api.transactions;

import com.alirabiee.apistats.data.Transaction;
import com.alirabiee.apistats.data.statistics.ExpiredTransactionException;
import com.alirabiee.apistats.data.statistics.FutureStampedTransactionException;
import com.alirabiee.apistats.data.statistics.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/transactions")
public class TransactionsController {
    private final Statistics statistics;

    @Autowired
    public TransactionsController(Statistics statistics) {
        this.statistics = statistics;
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity getStatistics(@RequestBody Transaction tx, HttpServletResponse response){
        try {
            statistics.add(tx);
            return new ResponseEntity(HttpStatus.CREATED);
        } catch (ExpiredTransactionException | FutureStampedTransactionException e) {
            return new ResponseEntity(HttpStatus.NO_CONTENT);
        }
    }
}
