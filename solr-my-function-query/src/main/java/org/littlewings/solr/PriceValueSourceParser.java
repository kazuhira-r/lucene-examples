package org.littlewings.solr;

import org.apache.lucene.queries.function.ValueSource;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.SyntaxError;
import org.apache.solr.search.ValueSourceParser;

public class PriceValueSourceParser extends ValueSourceParser {
    @Override
    public ValueSource parse(FunctionQParser fp) throws SyntaxError {
        ValueSource normalPrice = fp.parseValueSource();
        ValueSource campaignPrice = fp.parseValueSource();
        ValueSource startDate = fp.parseValueSource();
        ValueSource endDate = fp.parseValueSource();
        String now = fp.parseArg();

        return new PriceValueSource(normalPrice, campaignPrice, startDate, endDate, now);
    }
}
