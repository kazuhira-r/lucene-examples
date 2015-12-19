package org.littlewings.solr;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.queries.function.FunctionValues;
import org.apache.lucene.queries.function.ValueSource;
import org.apache.lucene.queries.function.docvalues.IntDocValues;

public class PriceValueSource extends ValueSource {
    protected ValueSource normalPrice;
    protected ValueSource campaignPrice;
    protected ValueSource startDate;
    protected ValueSource endDate;
    protected String now;

    public PriceValueSource(ValueSource normalPrice, ValueSource campaignPrice, ValueSource startDate, ValueSource endDate, String now) {
        this.normalPrice = normalPrice;
        this.campaignPrice = campaignPrice;
        this.startDate = startDate;
        this.endDate = endDate;
        this.now = now;
    }

    @Override
    public FunctionValues getValues(Map context, LeafReaderContext readerContext) throws IOException {
        FunctionValues normalPriceVals = normalPrice.getValues(context, readerContext);
        FunctionValues campaignPriceVals = campaignPrice.getValues(context, readerContext);
        FunctionValues startDateVals = startDate.getValues(context, readerContext);
        FunctionValues endDateVals = endDate.getValues(context, readerContext);

        return new IntDocValues(this) {
            @Override
            public int intVal(int doc) {
                if (startDateVals.exists(doc) && startDateVals.strVal(doc).compareTo(now) <= 0 &&
                        endDateVals.exists(doc) && endDateVals.strVal(doc).compareTo(now) >= 0) {
                    return campaignPriceVals.intVal(doc);
                } else {
                    return normalPriceVals.intVal(doc);
                }
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (PriceValueSource.class.equals(o.getClass())) {
            PriceValueSource other = (PriceValueSource) o;
            return Objects.equals(normalPrice, other.normalPrice) &&
                    Objects.equals(campaignPrice, other.campaignPrice) &&
                    Objects.equals(startDate, other.startDate) &&
                    Objects.equals(endDate, other.endDate) &&
                    Objects.equals(now, other.now);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(normalPrice, campaignPrice, startDate, endDate, now);
    }

    @Override
    public String description() {
        return PriceValueSource.class.getSimpleName();
    }
}
