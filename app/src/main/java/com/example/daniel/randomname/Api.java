package com.example.daniel.randomname;
        import java.util.List;

        import retrofit2.Call;
        import retrofit2.http.GET;
        import retrofit2.http.Path;
        import retrofit2.http.Query;

public interface Api {

    @GET("/api")
    Call<Person> getPerson(@Query("region") String region);
}
