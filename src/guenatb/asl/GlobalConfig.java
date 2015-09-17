package guenatb.asl;

/**
 * Created by Balz Guenat on 17.09.2015.
 */
public final class GlobalConfig {

    public static final int BODY_SIZE_LOWER_BOUND = 200;
    public static final int BODY_SIZE_UPPER_BOUND = 2000;

    public static final String DB_URL = "jdbc:postgresql://localhost:5432/asl";
    public static final String DB_USER = "postgres";
    public static final String DB_PASSWORD = "ham";

    public static final String FIRST_MIDDLEWARE_HOST = "localhost";
    public static final int FIRST_MIDDLEWARE_PORT= 13131;

}