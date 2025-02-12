package me.tongyifan.util;

import com.google.gson.Gson;
import me.tongyifan.entity.BaseResponse;
import okhttp3.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @author tongyifan
 */
public class Request {
    private final OkHttpClient okHttpClient = new OkHttpClient();
    private final Config config;

    public Request(Config config) {
        this.config = config;
    }

    public ImmutablePair<Boolean, List<String>> bindUser(String username, String passkey, long qq) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("type", "link_by_uid");
        params.put("platform", "qq");
        params.put("platform_id", Long.toString(qq));
        params.put("username", username);
        params.put("passkey", passkey);

        final Map<String, String> finalParams = prepareRequest(params);

        HttpUrl.Builder httpBuilder = Objects.requireNonNull(HttpUrl.parse(config.getBaseUrl() + "api_social_media.php")).newBuilder();

        okhttp3.Request request = new okhttp3.Request.Builder().url(httpBuilder.build()).post(RequestBody.create(new Gson().toJson(finalParams), MediaType.parse("application/json; charset=utf-8"))).build();

        BaseResponse responseObject;
        try {
            Response response = okHttpClient.newCall(request).execute();
            String responseBody = Objects.requireNonNull(response.body()).string();
            responseBody = responseBody.replace(":false,", ":0,").replace(":true,", ":1,");
            responseObject = new Gson().fromJson(responseBody, BaseResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return ImmutablePair.of(false, null);
        }

        if (responseObject.getStatus() == 0) {
            return ImmutablePair.of(true, null);
        } else if (responseObject.getStatus() == 1) {
            // 绑定成功，但存在警告
            return ImmutablePair.of(true, castList(responseObject.getData(), String.class));
        } else if (responseObject.getStatus() == -1) {
            return ImmutablePair.of(false, null);
        } else {
            return ImmutablePair.of(false, Collections.singletonList(responseObject.getMsg()));
        }
    }

    private Map<String, String> prepareRequest(final Map<String, String> params) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(config.getToken());
        params.keySet().forEach(key -> stringBuilder.append(params.get(key)));
        stringBuilder.append(config.getSecret());

        byte[] calcParams = stringBuilder.toString().getBytes(StandardCharsets.UTF_8);

        String sign = "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            sign = String.format("%032x", new BigInteger(1, md.digest(calcParams)));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        params.put("token", config.getToken());
        params.put("sign", sign);

        return params;
    }

    public static <T> List<T> castList(Object obj, Class<T> clazz) {
        List<T> result = new ArrayList<T>();
        if (obj instanceof List<?>) {
            for (Object o : (List<?>) obj) {
                result.add(clazz.cast(o));
            }
            return result;
        }
        return null;
    }
}
