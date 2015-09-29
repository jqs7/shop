package jadex.bdi.jqs7.shop;

import jadex.bdi.jqs7.shop.ItemInfo;
import jadex.bdi.runtime.Plan;

/**
 * Plan for selling an item.
 */
public class SellItemPlan extends Plan {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5173737675344658143L;

	/**
	 * The plan body.
	 */
	public void body() {
		// Fetch item data.
		// 获取商品属性值
		String name = (String) getParameter("name").getValue();
		double price = ((Double) getParameter("price").getValue()).doubleValue();
		ItemInfo ii = (ItemInfo) getBeliefbase().getBeliefSet("catalog").getFact(new ItemInfo(name));

		// Check if enough money is given and it is in stock.
		// 钱数与库存检查
		if (ii.getQuantity() > 0 && ii.getPrice() <= price) {
			// Sell item by updating catalog and account
			// System.out.println(getComponentName()+" sell item: "+name+" for:
			// "+price);
			getParameter("result").setValue(new ItemInfo(name, ii.getPrice(), 1));
			ii.setQuantity(ii.getQuantity() - 1);
			getBeliefbase().getBeliefSet("catalog").modified(ii);

			double money = ((Double) getBeliefbase().getBelief("money").getFact()).doubleValue();
			getBeliefbase().getBelief("money").setFact(Double.valueOf(money + price));
		} else if (ii.getQuantity() == 0) {// 库存为空
			throw new RuntimeException("Item not in store: " + name);
		} else {// 金钱不足
			throw new RuntimeException("Payment not sufficient: " + price);
		}
	}
}
