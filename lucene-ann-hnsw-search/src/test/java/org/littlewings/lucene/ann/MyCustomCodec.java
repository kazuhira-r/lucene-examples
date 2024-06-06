package org.littlewings.lucene.ann;

import org.apache.lucene.codecs.FilterCodec;
import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.codecs.lucene99.Lucene99Codec;
import org.apache.lucene.codecs.lucene99.Lucene99HnswVectorsFormat;
import org.apache.lucene.codecs.perfield.PerFieldKnnVectorsFormat;

public class MyCustomCodec extends FilterCodec {
    private KnnVectorsFormat knnVectorsFormat = new MyKnnVectorsFormat();

    public MyCustomCodec() {
        super("MyCustom", new Lucene99Codec());
    }

    @Override
    public KnnVectorsFormat knnVectorsFormat() {
        return knnVectorsFormat;
    }

    static class MyKnnVectorsFormat extends PerFieldKnnVectorsFormat {
        private KnnVectorsFormat defaultKnnVectorsFormat = new Lucene99HnswVectorsFormat();
        private KnnVectorsFormat forDescriptionVectorField = new Lucene99HnswVectorsFormat(32, 150);

        @Override
        public KnnVectorsFormat getKnnVectorsFormatForField(String field) {
            System.out.printf("KnnVectorsFormat#getKnnVectorsFormatForField = %s%n", field);

            if ("description_vector".equals(field)) {
                return forDescriptionVectorField;
            } else {
                return defaultKnnVectorsFormat;
            }
        }
    }
}
