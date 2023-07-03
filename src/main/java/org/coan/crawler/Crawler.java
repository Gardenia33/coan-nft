package org.coan.crawler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.coan.mapper.*;
import org.coan.pojo.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

@Component
public class Crawler {
    @Value("${crawler.url.baseUrl}")
    private String baseUrl;//基础url

    @Value("${crawler.url.marketUrl}")
    private String marketUrl;//市场url

    @Value("${crawler.url.infoUrl}")
    private String infoUrl;//信息url

    @Value("${crawler.news.baseUrl}")
    private String newsBaseUrl;//新闻基础url

    //注入相应mapper组件 用于操作数据表
    @Autowired
    private CoinMapper coinMapper;

    @Autowired
    private SectorMapper sectorMapper;

    @Autowired
    private CoinRecordMapper coinRecordMapper;

    @Autowired
    private CoanNewsMapper coanNewsMapper;

    @Autowired
    private CoanNewsContentMapper coanNewsContentMapper;

    public Crawler() {
    }

    //爬取coin
    public void crawlCoin() {
        HttpClient client = new HttpClient();
        HttpClientParams clientParams = client.getParams();
        clientParams.setContentCharset("UTF-8");

        //获取时间戳
        long curTime = System.currentTimeMillis();
        GetMethod method = new GetMethod(baseUrl + infoUrl + "?t=" + curTime);

        String response;
        int code;

        try {
            //执行get请求
            code = client.executeMethod(method);
            //response = method.getResponseBodyAsString();
            //读取响应内容
            InputStream in = method.getResponseBodyAsStream();
            BufferedReader reader = null;
            //响应头内容
            Header header = method.getResponseHeader("Content-Encoding");

            if (header != null) {
                //若响应内容被压缩，则进行解压处理
                if ("gzip".equals(header.getValue())) {
                    GZIPInputStream gis = new GZIPInputStream(in);
                    reader = new BufferedReader(new InputStreamReader(gis, "GBK"));
                }
            } else {//京东
                //按编码读取响应内容
                reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
            StringBuffer buffer = new StringBuffer();
            String info = reader.readLine();
            while (info != null) {
                buffer.append(info);
                buffer.append("\n");
                info = reader.readLine();
            }
            response = buffer.toString();


            if (code == HttpStatus.SC_OK) {
                //解析为json对象
                JSONObject object = JSON.parseObject(response);
                JSONArray jsonArray = object.getJSONObject("data").getJSONArray("list");
                //将json数组转换为java对象列表
                List<Coin> arr = jsonArray.toJavaList(Coin.class);
                for (Coin coin : arr) {

                    CoinRecord coinRecord = new CoinRecord();
                    //将coin对象的属性
                    BeanUtils.copyProperties(coin, coinRecord);
                    coinRecord.setTime(new Timestamp(curTime));
                    //插入coinR
                    // ecord的数据表
                    coinRecordMapper.insert(coinRecord);

                    //根据项目名称来查询coin数据表
                    LambdaQueryWrapper<Coin> lqw = new LambdaQueryWrapper<>();
                    lqw.eq(Coin::getProject, coin.getProject());

                    if (coinMapper.selectCount(lqw) == 0) {
                        //如果coin数据表中不存在相同项目的记录，则插入该记录
                        coinMapper.insert(coin);
                    } else {
                        //更新该记录
                        coinMapper.updateById(coin);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //爬取扇区列表数据
    public void crawlSectorList() {
        //创建httpclient实例 设置字符编码
        HttpClient client = new HttpClient();
        HttpClientParams clientParams = client.getParams();
        clientParams.setContentCharset("UTF-8");

        //构造GET请求对象
        GetMethod method = new GetMethod(baseUrl + marketUrl);

        String response;
        int code;

        try {
            //执行GET请求
            code = client.executeMethod(method);
            response = method.getResponseBodyAsString();

            //如果响应状态码为HttpStatus.SC_OK（200），则使用Jsoup解析响应内容为HTML文档。
            if (code == HttpStatus.SC_OK) {
                // 使用Jsoup解析响应内容为HTML文档
                Document doc = Jsoup.parse(response);
                // 从文档中获取具有"okui-tabs-pane"类名的元素集合
                Elements items = doc.getElementsByClass("okui-tabs-pane");

                for (Element element : items) {
                    //获取扇区名称

                    String sectorName = Objects.requireNonNull(element.firstElementChild()).text();
                    //获取扇区url
                    String sectorUrl = Objects.requireNonNull(element.firstElementChild()).attr("href");
//                    if("全部".equals(sectorName) || "主流币".equals(sectorName)) {
//                        continue;
//                    }


                    //利用getMaxPage获取扇区的最大页数
                    int page = getMaxPage(sectorUrl);
                    //遍历每一页进行爬取，调用crawlSectors
                    for (int i = 1; i <= page; i++) {
                        crawlSectors(sectorName, sectorUrl, i);
                    }

                }
            }
            //如果异常则打印堆栈
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //获取最大页数
    private int getMaxPage(String sectorUrl) {
        HttpClient client = new HttpClient();
        HttpClientParams clientParams = client.getParams();
        clientParams.setContentCharset("UTF-8");
        String fullUrl = baseUrl + sectorUrl;
        GetMethod method = new GetMethod(fullUrl);
        String response;
        int code;
        try {
            code = client.executeMethod(method);
            response = method.getResponseBodyAsString();

            if (code == HttpStatus.SC_OK) {
                Document doc = Jsoup.parse(response);
                Elements pages = doc.getElementsByClass("okui-pagination-item");
                int maxPage = 1;
                for (Element node : pages) {
                    maxPage = Math.max(Integer.parseInt(node.text()), maxPage);

                }
                return maxPage;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 1;
    }
    //爬取特定扇区下指定页数的数据
    private void crawlSectors(String sectorName, String sectorUrl, int page) {
        HttpClient client = new HttpClient();
        HttpClientParams clientParams = client.getParams();
        clientParams.setContentCharset("UTF-8");
        String fullUrl = baseUrl + sectorUrl + "?page=" + page;
        GetMethod method = new GetMethod(fullUrl);
        String response;
        int code;
        try {
            code = client.executeMethod(method);
            response = method.getResponseBodyAsString();

            if (code == HttpStatus.SC_OK) {
                Document doc = Jsoup.parse(response);
                //获取所有tbody元素
                Elements elements = doc.getElementsByTag("tbody");
                if (elements.isEmpty()) {
                    return;
                }
                //遍历tbody元素的子元素
                for (Element element : elements.get(0).children()) {

                    updateAndSave(sectorName, element);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //更新和保存数据
    private void updateAndSave(String sectorName, @NotNull Element element) {
        Sector sector = new Sector(sectorName, element.getElementsByClass("short-name").get(0).text());
        LambdaQueryWrapper<Sector> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Sector::getSectorName, sectorName).eq(Sector::getCoinName, sector.getCoinName());
        if (!sectorMapper.exists(lqw)) {
            sectorMapper.insert(sector);
        }
    }


    public void crawlNews() {
//        System.out.print(newsBaseUrl);

        String baseNews = "https://cn.investing.com/";
        HttpClient client = new HttpClient();
        HttpClientParams clientParams = client.getParams();
        clientParams.setContentCharset("UTF-8");
        //获取baseNEWsurl
        String newsUrl = newsBaseUrl;
        for (int i = 1; i < 10; i++) {
            //构造新闻url
            newsUrl = newsBaseUrl + "/" + i;
            // 构造GET请求对象
            GetMethod method = new GetMethod(newsUrl);
            // 设置请求头的User-Agent
            method.setRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0");
            String response;
            int code;
            try {
                //执行get请求
                code = client.executeMethod(method);
                response = method.getResponseBodyAsString();
                if (code == HttpStatus.SC_OK) {
                    Document doc = Jsoup.parse(response);
                    Element largeTitle = doc.getElementsByClass("largeTitle").get(0);

                    Elements items = largeTitle.getElementsByClass("js-article-item articleItem     ");
                    for (Element element : items) {
                        String articleUrl = Objects.requireNonNull(element.firstElementChild()).attr("href");
                        Elements x = element.getElementsByClass("articleDetails");
                        Element detail = x.get(0);
                        Elements spans = detail.getElementsByTag("span");
                        String provider = spans.get(1).text();
                        String date = spans.get(2).text();
                        String describe = element.getElementsByTag("p").text();
                        String title = element.getElementsByClass("title").get(0).attr("title");

//                    date=date.replace("- ","");
//                    provider=provider.replace("提供者 ","").replace(".","doTReplace")
//                            .replace("'","quatEReplace");
//                    articleUrl=articleUrl.replace("-","pounDReplace").replace("/","slasHReplace");
//                    describe=describe.replace("英为财情Investing.com - ","").replace(".","doTReplace")
//                            .replace(":","coloNReplace").replace("(","lefTReplace")
//                            .replace(")","righTReplace").replace("，","commAReplace");

                        date = date.replace("- ", "");
                        provider = provider.replace("提供者 ", "");
                        describe = describe.replace("英为财情Investing.com - ", "");
                        CoanNews news = new CoanNews();
                        news.setDescription(describe);
                        news.setProvider(provider);
                        news.setTitle(title);
                        news.setArticleUrl(articleUrl);
                        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy年MM月dd日");
                        Timestamp dateTime;
                        try {
                            dateTime = new Timestamp(dateFormatter.parse(date).getTime());
                        } catch (Exception e) {
                            dateTime = new Timestamp(System.currentTimeMillis());
                        }
                        news.setDate(dateTime);
                        Page<CoanNews> cur = coanNewsMapper.selectByUrl(articleUrl, new Page<>(0, 1));
                        if (cur.getRecords().isEmpty()) {
                            coanNewsMapper.insert(news);
                        }
//                        else {
//                            news.setId(cur.getRecords().get(0).getId());
//                            coanNewsMapper.updateById(news);
//                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void crawlNewsContent() {
        String baseNews = "https://cn.investing.com/";
        HttpClient client = new HttpClient();
        HttpClientParams clientParams = client.getParams();
        clientParams.setContentCharset("UTF-8");
        String newsUrl = newsBaseUrl;
        for (int i = 1; i < 10; i++) {
            newsUrl = newsBaseUrl + "/" + i;
            GetMethod method = new GetMethod(newsUrl);
            method.setRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0");
            String response;
            int code;
            try {
                code = client.executeMethod(method);
                response = method.getResponseBodyAsString();
                if (code == HttpStatus.SC_OK) {
                    Document doc = Jsoup.parse(response);
                    Element largeTitle = doc.getElementsByClass("largeTitle").get(0);
                    Elements items = largeTitle.getElementsByClass("js-article-item articleItem     ");
                    for (Element element : items) {
                        String articleUrl = Objects.requireNonNull(element.firstElementChild()).attr("href");
                        HttpClient client1 = new HttpClient();
                        HttpClientParams clientParams1 = client1.getParams();
                        clientParams1.setContentCharset("UTF-8");
                        GetMethod contentMethod = new GetMethod(baseNews + articleUrl);
                        contentMethod.setRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:50.0) Gecko/20100101 Firefox/50.0");
                        int contentCode;
                        String contentResponse;
                        try {

                            contentCode = client1.executeMethod(contentMethod);
                            contentResponse = contentMethod.getResponseBodyAsString();
                            if (contentCode == HttpStatus.SC_OK) {
                                Document CONdoc = Jsoup.parse(contentResponse);
                                Element articlePage = CONdoc.getElementsByClass("WYSIWYG articlePage").get(0);
                                Elements paragraphs = articlePage.getElementsByTag("p");
                                String pageContent = "";
                                for (Element para : paragraphs) {
                                    pageContent += para.text() + "\n";
                                }
                                CoanNewsContent content = new CoanNewsContent();
                                content.setUrl(articleUrl);
                                content.setContent(pageContent);
                                Page<CoanNewsContent> curContent = coanNewsContentMapper.selectByUrl(articleUrl, new Page<>(0, 1));
                                if (curContent.getRecords().isEmpty()) {
                                    coanNewsContentMapper.insert(content);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}


