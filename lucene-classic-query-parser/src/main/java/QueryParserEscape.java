import org.apache.lucene.queryparser.classic.QueryParser;

public class QueryParserEscape {
    public static void main(String[] args) {
        String input = "(1+1):2";
        System.out.println(QueryParser.escape(input));
    }
}