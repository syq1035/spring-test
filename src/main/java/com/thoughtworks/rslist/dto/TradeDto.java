package com.thoughtworks.rslist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "trade")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeDto {
    @Id
    @GeneratedValue
    private int id;
    private int amount;
    private int rank;
    private int rsEventId;
}
