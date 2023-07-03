package org.coan.controller;


import org.coan.service.CrawlerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/NFTArts")
public class CrawlerController {

    @Autowired
    CrawlerService CrawlerService;

    @GetMapping("/NFTArts")
    public void Crawler(){
        CrawlerService.crawlWebsite("");


    }

}