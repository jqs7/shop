package jadex.bdi.jqs7.shop;

import jadex.bdi.jqs7.shop.IShopService;
import jadex.bdi.jqs7.shop.ItemInfo;
import jadex.bdi.runtime.Plan;
import jadex.commons.future.IFuture;

/**
 * Buy a specific item in a given shop.
 */
public class BuyItemPlan extends Plan {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8916768100738945673L;

	/**
	 * The plan body.
	 */
	public void body() {
		// Fetch shop and item data
		// 获取商店服务
		IShopService shop = (IShopService) getParameter("shop").getValue();
		// 获取商品属性
		String name = (String) getParameter("name").getValue();
		double price = ((Double) getParameter("price").getValue()).doubleValue();
		double money = ((Double) getBeliefbase().getBelief("money").getFact()).doubleValue();

		// Check if enough money to buy the item
		if (money < price) // 检查金钱是否足够购买
			throw new RuntimeException("Not enough money to buy: " + name);

		// Buy the item at the shop (the shop is a service at another agent)
		// System.out.println(getComponentName()+" buying item: "+name);
		// 调用商品购买服务
		IFuture<ItemInfo> future = shop.buyItem(name, price);
		// System.out.println(getComponentName()+" getting item: "+future);
		// 获取购买结果信息
		ItemInfo item = (ItemInfo) future.get(this);
		// System.out.println(getComponentName()+" bought item: "+item);
		// 设置购买结果
		getParameter("result").setValue(item);

		// Update the customer inventory
		// 更新客户库存信息(已购买)
		ItemInfo ii = (ItemInfo) getBeliefbase().getBeliefSet("inventory").getFact(item);
		if (ii == null) {
			ii = new ItemInfo(name, price, 1);
			getBeliefbase().getBeliefSet("inventory").addFact(ii);
		} else {
			ii.setQuantity(ii.getQuantity() + 1);
			getBeliefbase().getBeliefSet("inventory").modified(ii);
		}

		// Update the account
		// Re-read money, could have changed due to executed sell plan
		// 修改账户资金
		money = ((Double) getBeliefbase().getBelief("money").getFact()).doubleValue();
		getBeliefbase().getBelief("money").setFact(Double.valueOf(money - price));
	}
}
