package org.sdb.buzzfeed.controller;

import org.sdb.buzzfeed.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka")
public class KafkaController {

    @Autowired
    private KafkaProducerService producer;

    @GetMapping("/send/{msg}")
    public String send(@PathVariable String msg) {
        producer.sendMessage(msg);
        return "消息已发送：" + msg;
    }
}
