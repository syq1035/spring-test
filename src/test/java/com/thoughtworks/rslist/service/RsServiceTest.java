package com.thoughtworks.rslist.service;

import com.thoughtworks.rslist.domain.Trade;
import com.thoughtworks.rslist.domain.Vote;
import com.thoughtworks.rslist.dto.RsEventDto;
import com.thoughtworks.rslist.dto.TradeDto;
import com.thoughtworks.rslist.dto.UserDto;
import com.thoughtworks.rslist.dto.VoteDto;
import com.thoughtworks.rslist.repository.RsEventRepository;
import com.thoughtworks.rslist.repository.TradeRepository;
import com.thoughtworks.rslist.repository.UserRepository;
import com.thoughtworks.rslist.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class RsServiceTest {
  RsService rsService;

  @Mock RsEventRepository rsEventRepository;
  @Mock UserRepository userRepository;
  @Mock VoteRepository voteRepository;
  @Mock TradeRepository tradeRepository;
  LocalDateTime localDateTime;
  Vote vote;
  UserDto userDto;
  RsEventDto rsEventDto;
  Trade trade;

  @BeforeEach
  void setUp() {
    initMocks(this);
    rsService = new RsService(rsEventRepository, userRepository, voteRepository, tradeRepository);
    localDateTime = LocalDateTime.now();
    vote = Vote.builder().voteNum(2).rsEventId(1).time(localDateTime).userId(1).build();
    userDto = UserDto.builder().voteNum(5).phone("18888888888").gender("female").email("a@b.com").age(19).userName("xiaoli").id(2).build();
    rsEventDto = RsEventDto.builder().eventName("event name").id(1).keyword("keyword").voteNum(2).user(userDto).build();
    trade = Trade.builder().amount(100).rank(2).build();
  }

  @Test
  void shouldVoteSuccess() {
    // given

    UserDto userDto =
        UserDto.builder()
            .voteNum(5)
            .phone("18888888888")
            .gender("female")
            .email("a@b.com")
            .age(19)
            .userName("xiaoli")
            .id(2)
            .build();
    RsEventDto rsEventDto =
        RsEventDto.builder()
            .eventName("event name")
            .id(1)
            .keyword("keyword")
            .voteNum(2)
            .user(userDto)
            .build();

    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
    when(userRepository.findById(anyInt())).thenReturn(Optional.of(userDto));
    // when
    rsService.vote(vote, 1);
    // then
    verify(voteRepository)
        .save(
            VoteDto.builder()
                .num(2)
                .localDateTime(localDateTime)
                .user(userDto)
                .rsEvent(rsEventDto)
                .build());
    verify(userRepository).save(userDto);
    verify(rsEventRepository).save(rsEventDto);
  }

  @Test
  void shouldThrowExceptionWhenUserNotExist() {
    // given
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.empty());
    when(userRepository.findById(anyInt())).thenReturn(Optional.empty());
    //when&then
    assertThrows(
        RuntimeException.class,
        () -> {
          rsService.vote(vote, 1);
        });
  }

  @Test
  void shouldBuyRsEventThrowExceptionWhenRsEventNotExist() {
    // given
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.empty());
    when(tradeRepository.findByRank(anyInt())).thenReturn(null);
    //when&then
    assertThrows(
            RuntimeException.class,
            () -> {
              rsService.buy(trade, 1);
            });
  }

  @Test
  void shouldBuyRsEventThrowExceptionWhenAmountIsNotEnough() {
    // given
    TradeDto tradeDto = TradeDto.builder().amount(666).rank(2).rsEventId(10).build();
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
    when(tradeRepository.findByRank(anyInt())).thenReturn(tradeDto);
    //when&then
    assertThrows(
            RuntimeException.class,
            () -> {
              rsService.buy(trade, 1);
            });
  }
  @Test
  void shouldbuyRsEventSuccessWhenTheRankIsNotBougth() {
    // given
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
    when(tradeRepository.findByRank(anyInt())).thenReturn(null);
    // when
    rsService.buy(trade, 1);
    //then
    verify(tradeRepository).save(TradeDto.builder().amount(trade.getAmount()).rank(trade.getRank()).rsEventId(1).build());
  }

  @Test
  void shouldbuyRsEventSuccessWhenTheRankIsBougthAndAmountEnough() {
    // given
    TradeDto tradeDto = TradeDto.builder().amount(66).rank(2).rsEventId(10).build();
    when(rsEventRepository.findById(anyInt())).thenReturn(Optional.of(rsEventDto));
    when(tradeRepository.findByRank(anyInt())).thenReturn(tradeDto);
    // when
    rsService.buy(trade, 1);
    //then
    verify(tradeRepository).delete(tradeDto);
    verify(tradeRepository).save(TradeDto.builder().amount(trade.getAmount()).rank(trade.getRank()).rsEventId(1).build());
  }
}
