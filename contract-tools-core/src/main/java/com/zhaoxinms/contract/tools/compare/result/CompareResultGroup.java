package com.zhaoxinms.contract.tools.compare.result;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class CompareResultGroup {
    List<CompareResult> results = new ArrayList<CompareResult>();
}
