import java.sql.*;
import java.nio.file.*;
import java.util.*;
import java.math.BigDecimal;

/** Standalone JDBC benchmark. Requires an EMPTY isolated only_perf_test database. Never drops tables. */
public class QueryBenchmark {
    static Connection db;
    static StringBuilder report = new StringBuilder();
    static final String live = " FROM game_trading WHERE is_deleted=0 AND sale_status='ON_SALE'";
    static void exec(String sql) throws Exception { try (var s=db.createStatement()) { s.execute(sql); } }
    static String rows(String sql) throws Exception {
        try(var s=db.createStatement(); var r=s.executeQuery(sql)) {
            var out=new StringBuilder(); int cols=r.getMetaData().getColumnCount();
            for(int i=1;i<=cols;i++) out.append(r.getMetaData().getColumnLabel(i)).append('\t'); out.append('\n');
            while(r.next()) { for(int i=1;i<=cols;i++) out.append(r.getString(i)).append('\t'); out.append('\n'); }
            return out.toString();
        }
    }
    static double timed(String sql) throws Exception {
        long start=System.nanoTime();
        try(var s=db.createStatement();var r=s.executeQuery(sql)) {
            while(r.next()) for(int i=1;i<=r.getMetaData().getColumnCount();i++) r.getObject(i);
        }
        return (System.nanoTime()-start)/1_000_000.0;
    }
    static void measure(String phase,int n) throws Exception {
        exec("ANALYZE TABLE game_trading, game_order");
        report.append("\n## ").append(n).append(" / ").append(phase).append("\n");
        var queries=new LinkedHashMap<String,String>();
        queries.put("time_first_original", "SELECT *"+live+" ORDER BY create_time DESC LIMIT 20");
        queries.put("time_first_stable", "SELECT *"+live+" ORDER BY create_time DESC,item_id DESC LIMIT 20");
        queries.put("price_asc_original", "SELECT *"+live+" ORDER BY price ASC,item_id DESC LIMIT 20");
        queries.put("price_asc", "SELECT *"+live+" ORDER BY price ASC,item_id ASC LIMIT 20");
        queries.put("price_desc", "SELECT *"+live+" ORDER BY price DESC,item_id DESC LIMIT 20");
        queries.put("price_range", "SELECT *"+live+" AND price BETWEEN 100 AND 200 ORDER BY price ASC,item_id ASC LIMIT 20");
        queries.put("keyword", "SELECT *"+live+" AND game_name LIKE '%006%' ORDER BY create_time DESC,item_id DESC LIMIT 20");
        queries.put("keyword_absent", "SELECT *"+live+" AND game_name LIKE '%absent%' ORDER BY create_time DESC,item_id DESC LIMIT 20");
        queries.put("count_range", "SELECT COUNT(*)"+live+" AND price BETWEEN 100 AND 200");
        queries.put("count_live", "SELECT COUNT(*)"+live);
        queries.put("count_keyword", "SELECT COUNT(*)"+live+" AND game_name LIKE '%006%'");
        queries.put("admin_time", "SELECT * FROM game_trading WHERE is_deleted=0 ORDER BY create_time DESC,item_id DESC LIMIT 20");
        queries.put("admin_price", "SELECT * FROM game_trading WHERE is_deleted=0 ORDER BY price ASC,item_id ASC LIMIT 20");
        int offset=n/2;
        queries.put("time_offset_deep", "SELECT *"+live+" ORDER BY create_time DESC,item_id DESC LIMIT 20 OFFSET "+offset);
        String boundary=rows("SELECT item_id"+live+" ORDER BY item_id DESC LIMIT 1 OFFSET "+(offset-1)).split("\n")[1].trim();
        queries.put("id_offset_deep", "SELECT *"+live+" ORDER BY item_id DESC LIMIT 20 OFFSET "+offset);
        queries.put("id_cursor_same_page", "SELECT *"+live+" AND item_id<"+boundary+" ORDER BY item_id DESC LIMIT 20");
        queries.put("id_cursor_fetch_extra", "SELECT *"+live+" AND item_id<"+boundary+" ORDER BY item_id DESC LIMIT 21");
        queries.put("user_orders", "SELECT * FROM game_order WHERE user_id=7 ORDER BY id DESC LIMIT 20");
        queries.put("count_user_orders", "SELECT COUNT(*) FROM game_order WHERE user_id=7");
        queries.put("order_detail", "SELECT * FROM game_order WHERE order_no='PERF0000000100'");
        queries.put("product_detail", "SELECT * FROM game_trading WHERE is_deleted=0 AND item_id=9991");
        queries.put("name_suggestions", "SELECT DISTINCT game_name FROM game_trading WHERE is_deleted=0 AND game_name LIKE '%007%' ORDER BY game_name LIMIT 20");
        if (!rows(queries.get("id_offset_deep")).equals(rows(queries.get("id_cursor_same_page"))))
            throw new AssertionError("Offset/cursor comparison must retrieve identical rows");
        for(var entry:queries.entrySet()) {
            String sql=entry.getValue(); for(int i=0;i<2;i++) timed(sql);
            double[] samples=new double[7];for(int i=0;i<7;i++)samples[i]=timed(sql);
            double[] sorted=samples.clone();Arrays.sort(sorted);
            report.append("\n### ").append(entry.getKey()).append("\n```sql\n").append(sql).append(";\n```\n")
                .append(String.format(Locale.ROOT,"ms: median=%.3f min=%.3f max=%.3f samples=%s%n",sorted[3],sorted[0],sorted[6],Arrays.toString(samples)))
                .append("```text\n").append(rows("EXPLAIN "+sql)).append(rows("EXPLAIN ANALYZE "+sql)).append("```\n");
        }
        Files.writeString(Path.of("sql/perf/results.md"),report.toString().replaceAll("(?m)[\\t ]+$", ""));
    }
    static void seed(int from,int to) throws Exception {
        db.setAutoCommit(false);
        try(var g=db.prepareStatement("INSERT INTO game_trading VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            var o=db.prepareStatement("INSERT INTO game_order VALUES (?, ?, ?, ?, ?, ?, 'PAID', 1700000000, 1700000001)")) {
            // V5 adds sale_status last. 5% deleted, 70% on-sale (65% live), repeated times/names.
            for(int i=from;i<=to;i++) {
                g.setLong(1,i);g.setInt(2,i%20==0?1:0);g.setString(3,"cover");g.setString(4,"wall");
                g.setString(5,String.format(Locale.ROOT,"Game%03d",i%100));g.setBigDecimal(6,BigDecimal.valueOf((i*7919L)%100000,2));
                g.setString(7,"description ".repeat(20));g.setInt(8,1700000000+i/100);g.setInt(9,1700000000+i/100);
                g.setString(10,i%10<7?"ON_SALE":i%10<9?"LOCKED":"SOLD");g.addBatch();
                o.setLong(1,i);o.setString(2,String.format(Locale.ROOT,"PERF%010d",i));o.setLong(3,i%5==0?7:i%1000+100);
                o.setLong(4,i);o.setString(5,"Game");o.setBigDecimal(6,BigDecimal.valueOf(i%1000));o.addBatch();
                if(i%1000==0){g.executeBatch();o.executeBatch();db.commit();}
            } g.executeBatch();o.executeBatch();db.commit();
        } finally {db.setAutoCommit(true);}
    }
    public static void main(String[] args) throws Exception {
        String url=System.getenv("ONLY_PERF_MYSQL_URL");
        if(url==null || !url.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/only_perf_test(?:_[a-z0-9]+)?(?:\\?.*)?"))
            throw new IllegalArgumentException("Requires localhost isolated only_perf_test");
        try(Connection c=DriverManager.getConnection(url,System.getenv().getOrDefault("ONLY_MYSQL_USER","root"),System.getenv().getOrDefault("ONLY_MYSQL_PASSWORD",""))) {
            db=c;
            try(var s=db.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE()")){
                r.next();if(r.getInt(1)!=0)throw new IllegalStateException("Benchmark requires empty schema; refuses to delete existing data");
            }
            String v3=Files.readString(Path.of("sql/v3_game_trading.sql"));v3=v3.substring(0,v3.indexOf("insert into"));
            String v4=Files.readString(Path.of("sql/v4_game_order.sql"));
            for(String sql:(v3+v4).replaceAll("(?m)^--.*$", "").split(";"))if(!sql.isBlank())exec(sql);
            String v5=Files.readString(Path.of("sql/v5_game_sale_status.sql"));
            exec(v5.substring(v5.indexOf("CREATE PROCEDURE"),v5.indexOf("END$$")+3));exec("CALL migrate_game_sale_status()");exec("DROP PROCEDURE migrate_game_sale_status");
            report.append("# Isolated MySQL query benchmark\n\nRun: ").append(java.time.Instant.now()).append("\n\n```text\n")
                .append(rows("SELECT DATABASE(), VERSION(), @@innodb_buffer_pool_size, @@transaction_isolation")).append("```\n")
                .append("\nJDBC wall time includes result retrieval; 2 warmups + 7 measured runs, single client, no concurrent writers.\n");
            for(int n:new int[]{10000,100000}) {
                seed(n==10000?1:10001,n);
                measure("baseline",n);
                String v6=Files.readString(Path.of("sql/v6_game_query_indexes.sql")).replaceAll("(?m)^--.*$", "");
                for(String sql:v6.split(";"))if(!sql.isBlank())exec(sql);
                measure("candidate",n);
                exec("DROP INDEX idx_game_sale_time ON game_trading");exec("DROP INDEX idx_game_sale_price ON game_trading");
            }
        }
        System.out.println("Wrote sql/perf/results.md; schema retained for inspection");
    }
}
