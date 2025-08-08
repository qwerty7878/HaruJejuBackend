package com.goodda.jejuday.crawler.service;

import com.goodda.jejuday.crawler.entitiy.JejuEvent;
import com.goodda.jejuday.crawler.repository.JejuEventRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class JejuEventCrawlerService {
    private final JejuEventRepository repository;
    private final DateTimeFormatter periodFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private final Pattern countPattern = Pattern.compile("(\\d+)");

    public JejuEventCrawlerService(JejuEventRepository repository) {
        this.repository = repository;
    }

    /**
     * 새로운 이벤트를 크롤링하여 저장하고, 저장된 엔티티 목록을 반환합니다.
     */
    @Transactional
    public List<JejuEvent> crawlNewEvents() throws Exception {
        List<JejuEvent> newEvents = new ArrayList<>();
        int page = 1;
        boolean foundExisting = false;
        String monthParam = String.format("%02d", LocalDate.now().getMonthValue());

        while (!foundExisting) {
            String url = String.format(
                    "https://www.visitjeju.net/kr/festival/list?month=%s&page=%d",
                    monthParam, page
            );
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            Elements items = doc.select("ul.event_list li.list_item");
            if (items.isEmpty()) break;

            List<Element> reversed = new ArrayList<>(items);
            Collections.reverse(reversed);

            for (Element item : reversed) {
                String href = item.selectFirst("a").attr("href");
                String contentsId = extractContentsId(href);
                if (repository.findByContentsId(contentsId).isPresent()) {
                    foundExisting = true;
                    break;
                }

                JejuEvent event = new JejuEvent();
                event.setContentsId(contentsId);
                event.setDetailUrl("https://www.visitjeju.net" + href);
                event.setImageUrl(item.selectFirst(".item_img img").absUrl("src"));
                event.setTitle(item.selectFirst(".item_tit").text());
                event.setSubTitle(item.selectFirst(".item_sub_tit").text());

                String period = item.selectFirst(".item_period").text().trim();
                String[] parts = period.split(" ~ ");
                event.setPeriodStart(LocalDate.parse(parts[0], periodFmt));
                event.setPeriodEnd(LocalDate.parse(parts[1], periodFmt));

                event.setLocation(item.selectFirst(".item_location").text());

                String countText = item.selectFirst(".item_count").text();
                Matcher matcher = countPattern.matcher(countText);
                if (matcher.find()) {
                    event.setLikesCount(Integer.parseInt(matcher.group(1)));
                }
                if (matcher.find()) {
                    event.setReviewsCount(Integer.parseInt(matcher.group(1)));
                }

                repository.save(event);
                newEvents.add(event);
            }

            if (!foundExisting) {
                page++;
            }
        }

        return newEvents;
    }

    /**
     * 첫 페이지에서 raw HTML li 요소들을 가져와 확인할 수 있는 리스트를 반환합니다.
     */
    public List<String> fetchRawLiHtml() throws IOException {
        String monthParam = String.format("%02d", LocalDate.now().getMonthValue());
        String url = String.format(
                "https://www.visitjeju.net/kr/festival/list?month=%s&page=1",
                monthParam
        );
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get();

        Elements items = doc.select("ul.event_list li.list_item");
        List<String> rawList = new ArrayList<>();
        for (Element item : items) {
            rawList.add(item.outerHtml());
        }
        return rawList;
    }

    private String extractContentsId(String href) {
        int start = href.indexOf("contentsid=") + "contentsid=".length();
        int end = href.indexOf('&', start);
        return (end > 0) ? href.substring(start, end) : href.substring(start);
    }
}