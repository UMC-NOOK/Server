package umc.nook.users.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QKakaoRefreshToken is a Querydsl query type for KakaoRefreshToken
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QKakaoRefreshToken extends EntityPathBase<KakaoRefreshToken> {

    private static final long serialVersionUID = -1043004501L;

    public static final QKakaoRefreshToken kakaoRefreshToken = new QKakaoRefreshToken("kakaoRefreshToken");

    public final StringPath accessToken = createString("accessToken");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath refreshToken = createString("refreshToken");

    public final NumberPath<Long> refreshTokenExpiresIn = createNumber("refreshTokenExpiresIn", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QKakaoRefreshToken(String variable) {
        super(KakaoRefreshToken.class, forVariable(variable));
    }

    public QKakaoRefreshToken(Path<? extends KakaoRefreshToken> path) {
        super(path.getType(), path.getMetadata());
    }

    public QKakaoRefreshToken(PathMetadata metadata) {
        super(KakaoRefreshToken.class, metadata);
    }

}

