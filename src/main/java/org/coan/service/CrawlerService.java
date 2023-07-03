package org.coan.service;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CrawlerService {

    public void crawlWebsite(String url) {
        try {
            // 使用Jsoup连接到指定的URL并获取文档对象
            Document document = Jsoup.connect(url).get();

            // 获取网页标题并打印
            String title = document.title();
            System.out.println( title);

            // 获取所有的链接元素并打印
            Elements links = document.select("a[href]");
            //System.out.println();
            for (Element link : links) {
                String linkUrl = link.attr("href");
                System.out.println(linkUrl);
            }
        } catch ( IOException e) {
            e.printStackTrace();
        }
    }
}
