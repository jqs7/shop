package jadex.bdi.jqs7.shop;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import com.alibaba.fastjson.JSON;

import jadex.bdi.jqs7.shop.ItemInfo;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.future.IFuture;
import jadex.commons.gui.SGUI;
import jadex.commons.transformation.annotations.Classname;
import redis.clients.jedis.Jedis;

public class ShopFrame extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2896663084035831236L;

	protected String shopName;

	public ShopFrame(final IBDIExternalAccess agent) {
		super(agent.getComponentIdentifier().getName());

		add(new ShopPanel(agent));
		pack();
		setLocation(SGUI.calculateMiddlePosition(this));
		setVisible(true);

		agent.scheduleStep(new IComponentStep<Void>() {
			@Classname("initialName")
			@Override
			public IFuture<Void> execute(IInternalAccess ia) {
				IBDIInternalAccess bia = (IBDIInternalAccess) ia;
				shopName = (String) bia.getBeliefbase().getBelief("shopname").getFact();
				return IFuture.DONE;
			}
		});

		// 窗口关闭监听
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {

				agent.scheduleStep(new IComponentStep<Void>() {
					@Override
					public IFuture<Void> execute(IInternalAccess ia) {
						IBDIInternalAccess bia = (IBDIInternalAccess) ia;
						// 获取信念集
						ItemInfo[] ii = (ItemInfo[]) bia.getBeliefbase().getBeliefSet("catalog").getFacts();
						// 链接数据库
						Jedis jedis = new Jedis("localhost");
						jedis.auth("xxxx");
						String s = JSON.toJSONString(ii);
						// 信念集信息转为JSON后存储到数据库
						jedis.set("shopdata:" + shopName, s);
						return IFuture.DONE;
					}
				});

				agent.killComponent();
			}
		});
	}
}
