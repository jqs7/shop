package jadex.bdi.jqs7.shop;

import jadex.bdi.jqs7.shop.IShopService;
import jadex.bdi.jqs7.shop.ItemInfo;
import jadex.bdi.runtime.AgentEvent;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IGoalListener;
import jadex.bridge.service.annotation.Service;
import jadex.bridge.service.annotation.ServiceComponent;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;

/**
 * The shop for buying goods at the shop.
 */
@Service
public class ShopService implements IShopService {
	// -------- attributes --------

	/** The component. */
	@ServiceComponent
	// BDI存取接口
	protected IBDIInternalAccess comp;

	/** The shop name. */
	protected String name;

	// -------- constructors --------

	/**
	 * Create a new shop service.
	 */
	public ShopService() {
		this.name = "noname-";
	}

	/**
	 * Create a new shop service.
	 */
	public ShopService(String name) {
		this.name = name;
	}

	// -------- methods --------

	/**
	 * Get the shop name.
	 * 
	 * @return The name.
	 * 
	 * @directcall (Is called on caller thread).
	 */
	public String getName() {
		return name;
	}

	/**
	 * Buy an item.
	 * 
	 * @param item
	 *            The item.
	 */
	// 购买物品
	public IFuture<ItemInfo> buyItem(final String item, final double price) {
		// 新建商品项目列表
		final Future<ItemInfo> ret = new Future<ItemInfo>();

		// 新建商品售卖目标
		final IGoal sell = comp.getGoalbase().createGoal("sell");
		// 目标参数设置(商品名称, 价格)
		sell.getParameter("name").setValue(item);
		sell.getParameter("price").setValue(Double.valueOf(price));
		// 目标监听器
		sell.addGoalListener(new IGoalListener() {
			// 当目标完成执行
			public void goalFinished(AgentEvent ae) {
				if (sell.isSucceeded())
					ret.setResult((ItemInfo) sell.getParameter("result").getValue());
				else
					ret.setException(sell.getException());
			}

			public void goalAdded(AgentEvent ae) {
			}
		});
		// 目标优先级调度设置
		comp.getGoalbase().dispatchTopLevelGoal(sell);

		return ret;
	}

	/**
	 * Get the item catalog.
	 * 
	 * @return The catalog.
	 */
	public IFuture<ItemInfo[]> getCatalog() {
		final Future<ItemInfo[]> ret = new Future<ItemInfo[]>();
		ret.setResult((ItemInfo[]) comp.getBeliefbase().getBeliefSet("catalog").getFacts());
		return ret;
	}

	/**
	 * Get the string representation.
	 * 
	 * @return The string representation.
	 */
	public String toString() {
		return name;
	}

}
