package app.nook.record.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRecordImage is a Querydsl query type for RecordImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRecordImage extends EntityPathBase<RecordImage> {

    private static final long serialVersionUID = -557289447L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRecordImage recordImage = new QRecordImage("recordImage");

    public final app.nook.global.common.QBaseEntity _super = new app.nook.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final StringPath key = createString("key");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedDate = _super.modifiedDate;

    public final NumberPath<Integer> orderIndex = createNumber("orderIndex", Integer.class);

    public final QRecord record;

    public QRecordImage(String variable) {
        this(RecordImage.class, forVariable(variable), INITS);
    }

    public QRecordImage(Path<? extends RecordImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRecordImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRecordImage(PathMetadata metadata, PathInits inits) {
        this(RecordImage.class, metadata, inits);
    }

    public QRecordImage(Class<? extends RecordImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.record = inits.isInitialized("record") ? new QRecord(forProperty("record"), inits.get("record")) : null;
    }

}

