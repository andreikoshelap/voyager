package ee.voyagelog.telegram.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdatesResponse(boolean ok, List<Update> result) {
}
