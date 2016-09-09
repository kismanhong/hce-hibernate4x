package softtech.hong.hce.model;

import org.apache.commons.lang3.ArrayUtils;

import softtech.hong.hce.type.OrderType;

/**
 * @author kismanhong
 * object order for defining order, Asc or Desc
 */
public class Order {
	private String property;
	
	private OrderType orderType;
	
	private Order[] orders;
	
	public Order(){}
	
	public Order(String property, OrderType orderType)
	{
		this.property = property;
		this.orderType = orderType;
		add(this);
	}

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public OrderType getOrderType() {
		return orderType;
	}

	public void setOrderType(OrderType orderType) {
		this.orderType = orderType;
	}
	
	public static Order asc(String propertyName){
		return new Order(propertyName, OrderType.Asc);
	}
	
	public static Order desc(String propertyName){
		return new Order(propertyName, OrderType.Desc);
	}
	
	public Order[] getOrders() {
		return orders;
	}

	public void setOrders(Order[] orders) {
		this.orders = orders;
	}

	public void add(Order order){
		orders = (Order[]) ArrayUtils.add(orders, order);
	}

}
