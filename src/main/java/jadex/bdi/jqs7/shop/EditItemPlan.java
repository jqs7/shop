package jadex.bdi.jqs7.shop;

import jadex.bdi.jqs7.shop.ItemInfo;
import jadex.bdi.runtime.Plan;

/**
 * Plan for selling an item.
 */
public class EditItemPlan extends Plan {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1408103009353695037L;

	/**
	 * The plan body.
	 */
	public void body() {
		// Fetch item data.
		// 获取商品属性值
		String name = (String) getParameter("name").getValue();
		String ename = (String) getParameter("ename").getValue();
		double price = ((Double) getParameter("price").getValue()).doubleValue();
		int quantity = (Integer) getParameter("quantity").getValue();
		if (name.equals("")) {// 新增商品
			ItemInfo ii = new ItemInfo(ename, price, quantity);
			getBeliefbase().getBeliefSet("catalog").addFact(ii);

		} else if (ename == null) {// 删除商品
			ItemInfo ii = (ItemInfo) getBeliefbase().getBeliefSet("catalog").getFact(new ItemInfo(name));
			getBeliefbase().getBeliefSet("catalog").removeFact(ii);
		} else {// 修改商品
			ItemInfo ii = (ItemInfo) getBeliefbase().getBeliefSet("catalog").getFact(new ItemInfo(name));
			ii.setName(ename);
			ii.setPrice(price);
			ii.setQuantity(quantity);
			getBeliefbase().getBeliefSet("catalog").modified(ii);
		}
	}
}
