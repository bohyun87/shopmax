package com.shopmax.service;

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import com.shopmax.dto.OrderDto;
import com.shopmax.dto.OrderHistDto;
import com.shopmax.dto.OrderItemDto;
import com.shopmax.entity.Item;
import com.shopmax.entity.ItemImg;
import com.shopmax.entity.Member;
import com.shopmax.entity.Order;
import com.shopmax.entity.OrderItem;
import com.shopmax.repository.ItemImgRepository;
import com.shopmax.repository.ItemRepository;
import com.shopmax.repository.MemberRepository;
import com.shopmax.repository.OrderRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {
	private final ItemRepository itemRepository;
	private final MemberRepository memberRepository;
	private final OrderRepository orderRepository;
	private final ItemImgRepository itemImgRepository;
	
	//주문하기
	public Long order(OrderDto orderDto, String email) {
		
		 //1. 주문할 상품 조회
		Item item = itemRepository.findById(orderDto.getItemId())   
								.orElseThrow(EntityNotFoundException::new);  //findById 를 사용할 때는 orElseThrow(EntityNotFoundException::new) 로 예외처리
		
		//2. 현재 로그인한 회원의 이메일을 이용해 회원정보를 조회
		Member member = memberRepository.findByEmail(email);
		
		//3. 주문할 상품 엔티티와 주문 수량을 이용하여 주문상품 엔티티를 생성
		List<OrderItem> orderItemList = new ArrayList<>();
		OrderItem orderItem = OrderItem.createOrderItem(item, orderDto.getCount());
		orderItemList.add(orderItem);   //Order 로 보낼건데 Order 에서는 OrderItem 을  List로 가지고 있음
		
		//4. 회원 정보와 주문할 상품 리스트 정보를 이용하여 주문 엔티티를 생성
		Order order = Order.createOrder(member, orderItemList);
		orderRepository.save(order); //insert
		
		return order.getId();		
	}
	
	//주문목록을 뽑는 서비스 메소드
	@Transactional(readOnly = true)
	public Page<OrderHistDto> getOrderList(String email, Pageable pageable) {
		
		//1. 유저 아이다와 페이징 조건을 이용하여 주문 목록을 조회
		List<Order> orders = orderRepository.findOrders(email, pageable);  //아이디별 주문 목록 조회
				
		//2. 유저의 주문 총 개수를 구한다.
		Long totalCount = orderRepository.countOrder(email);
		
		//3. 주문 리스트를 순회하면서 구매이력 페이지에 전달할 DTO 를 생성
		List<OrderHistDto> orderHistDtoList = new ArrayList<>();  //주문내역을 담을 구매이력 List 객체 생성 
		
		//주문상품 가져오기
		for(Order order : orders) {  // 아이디별 주문목록을 낱개로 풀기
			OrderHistDto orderHistDto = new OrderHistDto(order); //풀어놓은 주문내역을 구매이력 List 에 넣어주기
			List<OrderItem> orderItems = order.getOrderItems();  //주문한 내역의 상품 목록 조회
						
			for(OrderItem orderItem : orderItems) {  //주문내역에 있는 상품 목록을 낱개로 풀어주기
				//주문한 상품의 대표이미지를 찾아서 itemImg 에 담기
				ItemImg itemImg = itemImgRepository.findByItemIdAndRepimgYn(orderItem.getItem().getId(), "Y"); 
				OrderItemDto orderItemDto = new OrderItemDto(orderItem, itemImg.getImgUrl());
				orderHistDto.addOrderItemDto(orderItemDto);
			}
			
			orderHistDtoList.add(orderHistDto);
		}
		//4. 페이지 구현 객체를 생성하여 return		
		return new PageImpl<>(orderHistDtoList, pageable, totalCount);		 
	}
	
	
	//본인확인(현재 로그인한 사용자와 주문데이터를 생성한 사용자가 같은지 검사)
	@Transactional(readOnly = true)
	public boolean validateOrder(Long orderId, String email) {
		Member curMember = memberRepository.findByEmail(email);  //로그인한 사용자 찾기
		Order order = orderRepository.findById(orderId)
								.orElseThrow(EntityNotFoundException::new);
		
		Member savedMember = order.getMember();  //주문한 사용자 찾기
				
		//로그인한 사용자의 이메일과 주문한 사용자의 이메일이 같은지 최종비교
		if(!StringUtils.equals(curMember.getEmail(), savedMember.getEmail())){
			//같지 않으면
			return false;
		}
		
		return true;
		
	}
	
	//주문취소
	public void cancelOrder(Long orderId) {
		Order order = orderRepository.findById(orderId)
								.orElseThrow(EntityNotFoundException::new);
		
		//OrderStatus 를  update -> entity 의 필드 값을 바꿔주면 된다.
		order.cancelOrder();
		
	}
	
	
	//주문삭제
	public void deleteOrder(Long orderId) {
		//★delete 하기 전에 select 를 한번 해준다.
			// -> 영속성 컨텍스트에 엔티티를 저장한 후 변경 감지를 하도록 하기 위해
		Order order = orderRepository.findById(orderId)
									.orElseThrow(EntityNotFoundException::new);
		
		//JPA 에서 delete 하는 방법
		orderRepository.delete(order);   //
	}
}














