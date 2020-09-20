package com.thoughtworks.rslist.api;

import com.thoughtworks.rslist.domain.RsEvent;
import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.exception.Error;
import com.thoughtworks.rslist.exception.RequestNotValidException;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.service.RsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Validated
public class RsController {
  @Autowired RsEventRepository rsEventRepository;
  @Autowired UserRepository userRepository;
  @Autowired TradeRepository tradeRepository;
  @Autowired RsService rsService;

  @GetMapping("/rs/list")
  public ResponseEntity<List<RsEvent>> getRsEventListBetween(
      @RequestParam(required = false) Integer start, @RequestParam(required = false) Integer end) {
    List<RsEventDto> rsEventDtoList = rsEventRepository.findAll();
    List<TradeDto> tradeDtoList = tradeRepository.findAll();
    HashMap<Integer, Integer> trades = new HashMap<Integer, Integer>();
    tradeDtoList.stream().map(tradeDto -> trades.put(tradeDto.getRsEventId(), tradeDto.getRank())).collect(Collectors.toList());
    List<RsEventDto> rsEventsIsNotBuy = rsEventDtoList.stream()
            .filter(rsEvent -> !trades.containsKey(rsEvent.getId()))
            .sorted(new Comparator<RsEventDto>() {
              @Override
              public int compare(RsEventDto o1, RsEventDto o2) {
                int voteNumGap = o2.getVoteNum() - o1.getVoteNum();
                return voteNumGap > 0 ? voteNumGap : -1;
              }
            })
            .collect(Collectors.toList());
    List<RsEventDto> rsEventsIsBuy = rsEventDtoList.stream()
            .filter(rsEvent -> trades.containsKey(rsEvent.getId()))
            .collect(Collectors.toList());
    for(RsEventDto rsEventDto : rsEventsIsBuy) {
      int rank = trades.get(rsEventDto.getId());
      rsEventsIsNotBuy.add(rank-1, rsEventDto);
    }
    List<RsEvent> rsEvents = rsEventsIsNotBuy.stream().map(rsEventDto -> rsEventDtoToEntity(rsEventDto)).collect(Collectors.toList());
    if (start == null || end == null) {
      return ResponseEntity.ok(rsEvents);
    }
    return ResponseEntity.ok(rsEvents.subList(start - 1, end));
  }
public RsEvent rsEventDtoToEntity(RsEventDto rsEventDto) {
    return RsEvent.builder()
            .eventName(rsEventDto.getEventName())
            .keyword(rsEventDto.getKeyword())
            .userId(rsEventDto.getUser().getId())
            .voteNum(rsEventDto.getVoteNum())
            .build();
}
  @GetMapping("/rs/{index}")
  public ResponseEntity<RsEvent> getRsEvent(@PathVariable int index) {
    List<RsEvent> rsEvents =
        rsEventRepository.findAll().stream()
            .map(
                item ->
                    RsEvent.builder()
                        .eventName(item.getEventName())
                        .keyword(item.getKeyword())
                        .userId(item.getId())
                        .voteNum(item.getVoteNum())
                        .build())
            .collect(Collectors.toList());
    if (index < 1 || index > rsEvents.size()) {
      throw new RequestNotValidException("invalid index");
    }
    return ResponseEntity.ok(rsEvents.get(index - 1));
  }

  @PostMapping("/rs/event")
  public ResponseEntity addRsEvent(@RequestBody @Valid RsEvent rsEvent) {
    Optional<UserDto> userDto = userRepository.findById(rsEvent.getUserId());
    if (!userDto.isPresent()) {
      return ResponseEntity.badRequest().build();
    }
    RsEventDto build =
        RsEventDto.builder()
            .keyword(rsEvent.getKeyword())
            .eventName(rsEvent.getEventName())
            .voteNum(0)
            .user(userDto.get())
            .build();
    rsEventRepository.save(build);
    return ResponseEntity.created(null).build();
  }

  @PostMapping("/rs/vote/{id}")
  public ResponseEntity vote(@PathVariable int id, @RequestBody Vote vote) {
    rsService.vote(vote, id);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/rs/buy/{id}")
  public ResponseEntity buy(@PathVariable int id, @RequestBody Trade trade){
    rsService.buy(trade, id);
    return ResponseEntity.ok().build();
  }


  @ExceptionHandler(RequestNotValidException.class)
  public ResponseEntity<Error> handleRequestErrorHandler(RequestNotValidException e) {
    Error error = new Error();
    error.setError(e.getMessage());
    return ResponseEntity.badRequest().body(error);
  }
}
