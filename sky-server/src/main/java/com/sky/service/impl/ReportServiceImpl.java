package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 统计指定时间区间内的营业额数据
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>(); // 存放从begin到end范围内的每天日期
        while (!begin.equals(end)) {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);

        List<Double> turnoverList = new ArrayList<>(); // 每天的营业额
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); // yyyy-MM-dd 00:00:00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); // yyyy-MM-dd 23:59:59

            // 查询data日期对应的营业额，营业额是指状态为已完成的订单金额合计
            // select sum() from orders where order_time > ? and order_time < ? and status = 5
            Map<String, Object> map = new HashMap<>();
            map.put("begin", beginTime);
            map.put("end", endTime);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.sumByMap(map);

            turnover = (turnover == null ? 0.0 : turnover);
            turnoverList.add(turnover);
        }

        // 封装返回结果
        return TurnoverReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 用户统计接口
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>(); // 存放从begin到end范围内的每天日期
        while (!begin.equals(end)) {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);

        List<Integer> newUserList = new ArrayList<>(); // 新增用户数量
        List<Integer> totalUserList = new ArrayList<>(); // 总用户数量

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN); // yyyy-MM-dd 00:00:00
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX); // yyyy-MM-dd 23:59:59

            // select count(id) from user where create_time > beginTime and create_time < endTime
            Map<String, Object> map1 = new HashMap<>();
            map1.put("begin", beginTime);
            map1.put("end", endTime);
            Integer newUser = userMapper.countByMap(map1);

            // select count(id) from user where create_time < endTime
            Map<String, Object> map2 = new HashMap<>();
            map2.put("begin", null);
            map2.put("end", endTime);
            Integer totalUser = userMapper.countByMap(map2);

            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }

        return UserReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 订单统计接口
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>(); // 存放从begin到end范围内的每天日期
        while (!begin.equals(end)) {
            dateList.add(begin);
            begin = begin.plusDays(1);
        }
        dateList.add(end);

        List<Integer> orderCountList = new ArrayList<>(); // 每天的订单总数 列表
        List<Integer> validOrderCountList = new ArrayList<>(); // 每天有效订单总数 列表

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);

            // select count(*) from orders where order_time >= beginTime and order_time <= endTime
            Map<String, Object> map1 = new HashMap<>();
            map1.put("begin", beginTime);
            map1.put("end", endTime);
            map1.put("status", null);
            Integer orderCount = orderMapper.getOrderCount(map1);
            orderCountList.add(orderCount);

            // select count(*) from orders where order_time >= beginTime and order_time <= endTime and status = 5
            Map<String, Object> map2 = new HashMap<>();
            map2.put("begin", beginTime);
            map2.put("end", endTime);
            map2.put("status", Orders.COMPLETED);
            Integer validOrderCount = orderMapper.getOrderCount(map2);
            validOrderCountList.add(validOrderCount);
        }

        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();

        // 订单完成率
        double orderCompletionRate = 0.0;
        if (totalOrderCount != 0) {
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }

        return OrderReportVO
                .builder()
                .dateList(StringUtils.join(dateList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .validOrderCount(validOrderCount)
                .orderCountList(StringUtils.join(orderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计指定时间区间的销量前10
     *
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        // select od.name sum(od.number) number
        // from order.detail od, orders o
        // where od.order_id = o.id and o.id = 5 and o.order_time > begin and o.order_time < end
        // group by od.name
        // order by number
        // limit 0,10

        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);
        String nameList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList()),",");
        String numberList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList()),",");

        return SalesTop10ReportVO
                .builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
}
