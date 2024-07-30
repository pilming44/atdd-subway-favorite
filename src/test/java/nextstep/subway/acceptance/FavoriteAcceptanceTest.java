package nextstep.subway.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.favorite.application.dto.FavoriteResponse;
import nextstep.subway.utils.AcceptanceTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nextstep.subway.acceptance.AcceptanceTestUtil.*;
import static nextstep.subway.acceptance.MemberSteps.회원_생성_요청;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("즐겨찾기 관련 기능")
public class FavoriteAcceptanceTest extends AcceptanceTest {
    public static final String EMAIL = "email@email.com";
    public static final String PASSWORD = "password";
    public static final int AGE = 20;

    private Long 교대역Id;
    private Long 강남역Id;
    private Long 양재역Id;
    private Long 남부터미널역Id;

    @BeforeEach
    void setUpData() {
        교대역Id = 역_생성("교대역").jsonPath().getLong("id");
        강남역Id = 역_생성("강남역").jsonPath().getLong("id");
        양재역Id = 역_생성("양재역").jsonPath().getLong("id");
        남부터미널역Id = 역_생성("남부터미널역").jsonPath().getLong("id");

        노선_생성_Extract(노선_생성_매개변수("2호선", "bg-green-600", 교대역Id, 강남역Id, 10L));
        노선_생성_Extract(노선_생성_매개변수("신분당선", "bg-gre-600", 강남역Id, 양재역Id, 10L));
        ExtractableResponse<Response> 삼호선_생성_응답 = 노선_생성_Extract(노선_생성_매개변수("3호선", "bg-green-600", 교대역Id, 남부터미널역Id, 2L));
        long 삼호선Id = 삼호선_생성_응답.jsonPath().getLong("id");
        노선에_새로운_구간_추가_Extract(구간_생성_매개변수(남부터미널역Id, 양재역Id, 3L), 삼호선Id);

        회원_생성_요청(EMAIL, PASSWORD, AGE);
    }
    /**
     * given 3개의 노선이 등록돼있고, (교대-강남 [10], 강남-양재 [10], 교대-남부터미널 [2], 남부터미널-양재 [3])
     *       인증정보를 가지고있다.
     * when 인증정보와 경로를 즐겨찾기로 등록하면
     * then 즐겨찾기로 등록된다.
     */
    @Test
    @DisplayName("즐겨찾기 정상 생성")
    void 즐겨찾기_생성() {
        // given
        String 인증_토큰 = 로그인_토큰_생성(EMAIL, PASSWORD, AGE);

        Map<String, String> 경로_매개변수 = 경로_매개변수(교대역Id, 양재역Id);
        // when
        ExtractableResponse<Response> 즐겨찾기_생성_응답_추출 = 즐겨찾기_생성(인증_토큰, 경로_매개변수);

        // then
        assertThat(즐겨찾기_생성_응답_추출.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    /**
     * given 계정에 즐겨찾기가 2개 등록돼있고
     * when 인증정보와 함께 즐겨찾기를 조회하면
     * then 즐겨찾기 정보가 리턴된다.
     */
    @Test
    @DisplayName("즐겨찾기 정상 조회")
    void 즐겨찾기_조회() {
        // given
        String 인증_토큰 = 로그인_토큰_생성(EMAIL, PASSWORD, AGE);
        Map<String, String> 교대_양재_매개변수 = 경로_매개변수(교대역Id, 양재역Id);
        Map<String, String> 강남_양재_매개변수 = 경로_매개변수(강남역Id, 양재역Id);
        즐겨찾기_생성(인증_토큰, 교대_양재_매개변수);
        즐겨찾기_생성(인증_토큰, 강남_양재_매개변수);

        // when
        ExtractableResponse<Response> 즐겨찾기_목록_응답 = 즐겨찾기_목록_조회(인증_토큰);

        // then
        List<FavoriteResponse> 즐겨찾기_목록 = 즐겨찾기_목록_응답.jsonPath().getList(".", FavoriteResponse.class);
        assertThat(즐겨찾기_목록).hasSize(2);
        FavoriteResponse 첫번째_즐겨찾기 = 즐겨찾기_목록.get(0);
        assertThat(첫번째_즐겨찾기.getSource().getId()).isEqualTo(교대역Id);
        assertThat(첫번째_즐겨찾기.getTarget().getId()).isEqualTo(양재역Id);

        FavoriteResponse 두번쨰_즐겨찾기 = 즐겨찾기_목록.get(1);
        assertThat(두번쨰_즐겨찾기.getSource().getId()).isEqualTo(강남역Id);
        assertThat(두번쨰_즐겨찾기.getTarget().getId()).isEqualTo(양재역Id);

    }

    /**
     * given 계정에 즐겨찾기가 2개 등록돼있고
     * when 인증정보와 함께 즐겨찾기를 삭제하면
     * then 즐겨찾기가 삭제된다.
     */
    @Test
    @DisplayName("즐겨찾기 정상 삭제")
    void 즐겨찾기_삭제() {
        // given
        String 인증_토큰 = 로그인_토큰_생성(EMAIL, PASSWORD, AGE);
        Map<String, String> 교대_양재_매개변수 = 경로_매개변수(교대역Id, 양재역Id);
        Map<String, String> 강남_양재_매개변수 = 경로_매개변수(강남역Id, 양재역Id);
        즐겨찾기_생성(인증_토큰, 교대_양재_매개변수);
        즐겨찾기_생성(인증_토큰, 강남_양재_매개변수);
        ExtractableResponse<Response> 즐겨찾기_목록_응답 = 즐겨찾기_목록_조회(인증_토큰);
        List<FavoriteResponse> 즐겨찾기_목록 = 즐겨찾기_목록_응답.jsonPath().getList(".", FavoriteResponse.class);
        Long 첫번째_즐겨찾기_Id = 즐겨찾기_목록.get(0).getId();
        Long 두번째_즐겨찾기_Id = 즐겨찾기_목록.get(1).getId();

        // when
        ExtractableResponse<Response> 즐겨찾기_삭제_응답 = RestAssured.given().log().all()
                .auth().oauth2(인증_토큰)
                .when().delete("/favorites/" + 첫번째_즐겨찾기_Id)
                .then().log().all()
                .extract();

        // then
        assertThat(즐겨찾기_삭제_응답.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());

        ExtractableResponse<Response> 삭제_후_즐겨찾기_목록_응답 = 즐겨찾기_목록_조회(인증_토큰);
        List<FavoriteResponse> 삭제_후_즐겨찾기_목록 = 삭제_후_즐겨찾기_목록_응답.jsonPath().getList(".", FavoriteResponse.class);
        assertThat(삭제_후_즐겨찾기_목록).hasSize(1);

        FavoriteResponse 즐겨찾기 = 삭제_후_즐겨찾기_목록.get(0);
        assertThat(즐겨찾기.getId()).isEqualTo(두번째_즐겨찾기_Id);
    }

    private static ExtractableResponse<Response> 즐겨찾기_목록_조회(String 인증_토큰) {
        return RestAssured.given().log().all()
                .auth().oauth2(인증_토큰)
                .when().get("/favorites")
                .then().log().all()
                .extract();
    }

    private ExtractableResponse<Response> 즐겨찾기_생성(String accessToken, Map<String, String> 경로_매개변수) {
        return RestAssured.given().log().all()
                .auth().oauth2(accessToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(경로_매개변수)
                .when().post("/favorites")
                .then().log().all()
                .extract();
    }

    private ExtractableResponse<Response> 로그인_토큰_생성_응답(Map<String, String> params) {
        return RestAssured.given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(params)
                .when().post("/login/token")
                .then().log().all()
                .statusCode(HttpStatus.OK.value()).extract();
    }

    private String 로그인_토큰_생성(String email, String password, int age) {
        Map<String, String> 로그인_매개변수 = 로그인_매개변수(email, password, String.valueOf(age));
        ExtractableResponse<Response> response = 로그인_토큰_생성_응답(로그인_매개변수);
        return response.jsonPath().getString("accessToken");
    }

    private Map<String, String> 로그인_매개변수(String email, String password, String age) {
        Map<String, String> params = new HashMap<>();
        params.put("email", EMAIL);
        params.put("password", PASSWORD);
        params.put("age", AGE + "");
        return params;
    }

    private Map<String, String> 경로_매개변수(Long source, Long target) {
        Map<String, String> param = new HashMap<>();
        param.put("source", source.toString());
        param.put("target", target.toString());
        return param;
    }
}