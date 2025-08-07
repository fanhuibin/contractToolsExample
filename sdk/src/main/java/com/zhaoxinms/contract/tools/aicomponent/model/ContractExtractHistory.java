package com.zhaoxinms.contract.tools.aicomponent.model;

import java.time.LocalDateTime;
import javax.persistence.*;

import lombok.Data;

@Data
@Entity
@Table(name = "contract_extract_history")
public class ContractExtractHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName; 

    @Lob
    @Column(nullable = false)
    private String extractedContent;

    @Column(nullable = false)
    private LocalDateTime extractTime;

    private String userId;
}

