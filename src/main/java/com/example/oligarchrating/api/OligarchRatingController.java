package com.example.oligarchrating.api;

import com.example.oligarchrating.api.dto.RatingRequest;
import com.example.oligarchrating.api.dto.RatingResponse;
import com.example.oligarchrating.service.OligarchRatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/oligarch-ratings", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Oligarch Rating", description = "Rate persons against the oligarch threshold")
public class OligarchRatingController {

    private final OligarchRatingService oligarchRatingService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rate a person and persist them if they qualify as an oligarch")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rating successfully computed"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "502", description = "Upstream service failure"),
            @ApiResponse(responseCode = "504", description = "Upstream service timeout")
    })
    public RatingResponse rate(@Valid @RequestBody RatingRequest request) {
        return oligarchRatingService.rate(request);
    }
}
