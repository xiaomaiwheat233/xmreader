package com.xmreader.catalog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.fixtures", name = "enabled", havingValue = "true")
public class FixtureDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FixtureDataInitializer.class);
    private static final String SOURCE_KEY = "fixture-original";

    private final ContentSourceMapper sourceMapper;
    private final BookMapper bookMapper;
    private final ChapterMapper chapterMapper;
    private final Clock clock;

    public FixtureDataInitializer(
            ContentSourceMapper sourceMapper,
            BookMapper bookMapper,
            ChapterMapper chapterMapper,
            Clock clock) {
        this.sourceMapper = sourceMapper;
        this.bookMapper = bookMapper;
        this.chapterMapper = chapterMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ContentSourceEntity source = sourceMapper.findBySourceKey(SOURCE_KEY);
        if (source == null) {
            source = createSource();
            sourceMapper.insert(source);
        }

        int inserted = 0;
        for (FixtureBook fixture : fixtures()) {
            BookEntity existing = bookMapper.selectOne(new LambdaQueryWrapper<BookEntity>()
                    .eq(BookEntity::getSourceId, source.getId())
                    .eq(BookEntity::getSourceBookId, fixture.key())
                    .last("LIMIT 1"));
            if (existing == null) {
                insertBook(source.getId(), fixture);
                inserted++;
            }
        }
        if (inserted > 0) {
            log.info("Loaded {} original fixture books for local development", inserted);
        }
    }

    private ContentSourceEntity createSource() {
        LocalDateTime now = now();
        ContentSourceEntity source = new ContentSourceEntity();
        source.setSourceKey(SOURCE_KEY);
        source.setDisplayName("小麦原创测试书库");
        source.setBaseUrl("https://fixture.invalid/");
        source.setAdapterType("FIXTURE");
        source.setEnabled(true);
        source.setCreatedAt(now);
        source.setUpdatedAt(now);
        return source;
    }

    private void insertBook(long sourceId, FixtureBook fixture) {
        LocalDateTime now = now().minusDays(fixture.ageInDays());
        String sourceUrl = "https://fixture.invalid/books/" + fixture.key();
        BookEntity book = new BookEntity();
        book.setSourceId(sourceId);
        book.setSourceBookId(fixture.key());
        book.setSourceUrl(sourceUrl);
        book.setSourceUrlHash(hash(sourceUrl));
        book.setTitle(fixture.title());
        book.setAuthor(fixture.author());
        book.setDescription(fixture.description());
        book.setCategory(fixture.category());
        book.setStatus(fixture.status());
        book.setVisibility("VISIBLE");
        book.setWordCount(0L);
        book.setChapterCount(0);
        book.setLastCrawledAt(now);
        book.setCreatedAt(now);
        book.setUpdatedAt(now);
        bookMapper.insert(book);

        long totalWords = 0;
        ChapterEntity latest = null;
        for (int index = 0; index < fixture.chapters().size(); index++) {
            FixtureChapter fixtureChapter = fixture.chapters().get(index);
            String chapterUrl = sourceUrl + "/chapters/" + (index + 1);
            ChapterEntity chapter = new ChapterEntity();
            chapter.setBookId(book.getId());
            chapter.setChapterIndex(index + 1);
            chapter.setTitle(fixtureChapter.title());
            chapter.setContent(fixtureChapter.content());
            chapter.setContentHash(hash(fixtureChapter.content()));
            chapter.setSourceUrl(chapterUrl);
            chapter.setSourceUrlHash(hash(chapterUrl));
            chapter.setWordCount(fixtureChapter.content().replaceAll("\\s", "").length());
            chapter.setPublishedAt(now.plusHours(index));
            chapter.setCreatedAt(now.plusHours(index));
            chapter.setUpdatedAt(now.plusHours(index));
            chapterMapper.insert(chapter);
            totalWords += chapter.getWordCount();
            latest = chapter;
        }

        book.setWordCount(totalWords);
        book.setChapterCount(fixture.chapters().size());
        book.setLatestChapterId(latest == null ? null : latest.getId());
        book.setLatestChapterTitle(latest == null ? null : latest.getTitle());
        book.setUpdatedAt(latest == null ? now : latest.getUpdatedAt());
        bookMapper.updateById(book);
    }

    private List<FixtureBook> fixtures() {
        return List.of(
                new FixtureBook(
                        "letters-from-the-wheat-field",
                        "麦田来信",
                        "小麦编辑部",
                        "三个离开故乡的年轻人，通过一只旧邮箱重新听见土地与季节的声音。",
                        "现实",
                        "COMPLETED",
                        1,
                        List.of(
                                new FixtureChapter("第一章 风把信送来", """
                                        九月的风越过河堤，把麦香送进了城南邮局。许舟整理最后一袋信时，发现一只没有邮票的牛皮信封，上面只写着他的名字。

                                        信纸很薄，字迹却熟悉：今年的麦子已经收好，院角那株石榴结了七个果。母亲没有催他回家，只在末尾画了一条弯弯的小路。

                                        他把信折回去，第一次认真看见窗外的晚霞。那条小路似乎从纸上延伸出来，穿过楼群，一直通向北方。"""),
                                new FixtureChapter("第二章 河堤上的灯", """
                                        下班后，许舟沿河慢慢走。路灯一盏接一盏亮起，水面把光揉成细碎的金线。

                                        他给多年未见的好友林乔打了电话。两个人起初只谈天气，后来谈到废弃的校舍、旧操场和小时候埋在槐树下的玻璃瓶。

                                        林乔说，回去看看吧，不一定要留下，但该知道来时的路还在。"""),
                                new FixtureChapter("第三章 秋收之后", """
                                        周末清晨，慢车停在小站。许舟背着轻便的包，远远看见母亲站在银杏树下，手里没有伞，也没有招牌。

                                        田野已经收割，只剩整齐的麦茬。空旷并不意味着结束，泥土下面，新一季的种子正安静等待。

                                        晚饭后，他把回信投入院角的旧邮箱。收信人写的是未来的自己：愿你走得再远，也能听见风从麦田吹来。"""))),
                new FixtureBook(
                        "lighthouse-in-mist-harbor",
                        "雾港灯塔",
                        "林汀",
                        "守灯人和气象员在漫长海雾中修复灯塔，也拼起一段被潮水隐藏的往事。",
                        "悬疑",
                        "ONGOING",
                        2,
                        List.of(
                                new FixtureChapter("第一章 无声的汽笛", """
                                        雾从凌晨开始封住港口。气象员沈汀记录能见度时，听见海面传来一声汽笛，低沉、悠长，却没有任何船只出现在雷达上。

                                        老守灯人把窗扣紧，只说那是风钻进礁石的声音。可沈汀知道，风不会严格地每隔十七分钟重复一次。

                                        她在记录簿上写下时间，并在第三声汽笛后走向灯塔顶层。"""),
                                new FixtureChapter("第二章 缺失的值班页", """
                                        灯塔的旧档案按年份装订，唯独十年前秋季少了三页。撕口很新，纸屑还夹在铜钉附近。

                                        沈汀用手电斜照封面，压痕里浮出一个船名：白鹭号。港务记录却显示，这艘船从未在雾港注册。

                                        楼下传来钥匙碰撞的声音。老守灯人站在门口，第一次问她究竟在找什么。"""),
                                new FixtureChapter("第三章 灯光转向", """
                                        午夜，主灯忽然偏离航道，光束扫过北侧废弃的礁滩。那里短暂反射出三点冷光，像有人按约定举起镜子。

                                        沈汀切换备用电机，把方位恢复。她没有报警，而是带上潮汐表和救生绳，从维修梯下到礁滩。

                                        雾里，一只生锈的信号箱正随着浪声轻轻震动。"""))),
                new FixtureBook(
                        "postman-among-stars",
                        "星河邮差",
                        "周野",
                        "一名星际邮差驾驶老旧飞船，为失去通信的边缘行星送去最后一批纸质信件。",
                        "科幻",
                        "COMPLETED",
                        3,
                        List.of(
                                new FixtureChapter("第一章 最慢的航线", """
                                        联盟通信网覆盖了九成九的居住区，纸质邮件因此成了博物馆里的东西。程野的飞船却仍挂着邮政编号，沿最慢的航线飞行。

                                        货舱里只有十二封信，目的地是失联六年的青禾星。导航系统建议返航，他把提示调成静音，继续驶向暗红色的恒星。

                                        有些话能够瞬间抵达，有些话必须由一个人亲手送到。"""),
                                new FixtureChapter("第二章 沉睡的收件箱", """
                                        青禾星的轨道站没有回应。程野手动对接，在积尘的大厅里找到十二只机械收件箱。

                                        第一封信被投入时，整座大厅亮起微弱蓝光。旧系统逐个核对姓名，并投射出收件人的最后登记地址。

                                        十二个地址都指向同一处：北纬三十七度的种子穹顶。"""),
                                new FixtureChapter("第三章 给明天的回信", """
                                        穹顶里没有居民，只有休眠的植物胚芽和一台仍在工作的记录仪。六年前，殖民者在撤离前把希望留在这里。

                                        程野读完授权信息，将十二封信放进恒温柜。它们的收件人尚未出生，要等生态修复完成后才会拥有姓名。

                                        离开前，他替未来的孩子写下一张明信片：宇宙很大，但总有人记得给你留一盏灯。"""))));
    }

    private byte[] hash(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private record FixtureBook(
            String key,
            String title,
            String author,
            String description,
            String category,
            String status,
            int ageInDays,
            List<FixtureChapter> chapters) {
    }

    private record FixtureChapter(String title, String content) {
    }
}
