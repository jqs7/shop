package jadex.bdi.jqs7.shop;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.jfree.ui.tabbedui.VerticalLayout;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import jadex.bdi.jqs7.shop.ItemInfo;
import jadex.bdi.jqs7.shop.ItemTableModel;
import jadex.bdi.runtime.AgentEvent;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bdi.runtime.IBeliefSetListener;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IGoalListener;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.SUtil;
import jadex.commons.future.IFuture;
import jadex.commons.gui.SGUI;
import jadex.commons.transformation.annotations.Classname;
import redis.clients.jedis.Jedis;

public class ShopPanel extends JPanel {
	private static final long serialVersionUID = -1053469252614466558L;
	protected IBDIExternalAccess agent;
	protected List shopList = new ArrayList();
	protected JTable shopTable;
	protected String shopName;
	protected AbstractTableModel shopModel = new ItemTableModel(shopList);

	public ShopPanel(final IBDIExternalAccess agent) {
		this.agent = agent;

		// 商店名
		JPanel namePanel = new JPanel();
		final JTextField nameField = new JTextField();
		nameField.setEditable(false);
		agent.scheduleStep(new IComponentStep<Void>() {
			@Classname("initialName")
			@Override
			public IFuture<Void> execute(IInternalAccess ia) {
				IBDIInternalAccess bia = (IBDIInternalAccess) ia;
				// 获取商店名信念
				shopName = (String) bia.getBeliefbase().getBelief("shopname").getFact();
				// 文本域的值设置为商店名
				nameField.setText(shopName);
				return IFuture.DONE;
			}
		});
		namePanel.add(new JLabel("Shop Name:"));
		namePanel.add(nameField);

		// 商品信息
		JPanel shopPanel = new JPanel();
		shopTable = new JTable(shopModel);
		// 滚动模式与单选模式
		shopTable.setPreferredScrollableViewportSize(new Dimension(600, 120));
		shopTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		// 初始化商品信息
		agent.scheduleStep(new IComponentStep<Void>() {
			@Override
			@Classname("initItem")
			public IFuture<Void> execute(IInternalAccess ia) {
				IBDIInternalAccess bia = (IBDIInternalAccess) ia;
				// 连接数据库
				Jedis jedis = new Jedis("localhost");
				// jedis.auth("*****");
				String s = jedis.get("shopdata:" + shopName);
				List<ItemInfo> obj = null;
				if (s != null) {
					// 将JSON信息转回JAVA对象
					obj = JSON.parseArray(s, ItemInfo.class);
				}
				if (obj != null) {
					// 将数据库中取出的信息加入信念集
					bia.getBeliefbase().getBeliefSet("catalog").addFacts(obj.toArray());
				}

				// 信念集查询
				ItemInfo[] ii = (ItemInfo[]) bia.getBeliefbase().getBeliefSet("catalog").getFacts();

				// 更新商品信息
				shopList.clear();
				for (int i = 0; i < ii.length; i++) {
					shopList.add(ii[i]);
				}
				shopModel.fireTableDataChanged();
				return IFuture.DONE;
			}
		});

		// 商品列表与信念集同步
		agent.scheduleStep(new IComponentStep<Void>() {
			@Override
			@Classname("shopItem")
			public IFuture<Void> execute(IInternalAccess ia) {
				IBDIInternalAccess bia = (IBDIInternalAccess) ia;
				// 信念集监听器
				bia.getBeliefbase().getBeliefSet("catalog").addBeliefSetListener(new IBeliefSetListener() {
					@Override
					// 当信念事实被移除
					public void factRemoved(final AgentEvent ae) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								shopList.remove(ae.getValue());
								shopModel.fireTableDataChanged();
							}
						});
					}

					@Override
					// 当信念事实被更改
					public void factChanged(final AgentEvent ae) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								shopList.remove(ae.getValue());
								shopList.add(ae.getValue());
								shopModel.fireTableDataChanged();
							}
						});
					}

					@Override
					// 当信念事实增加
					public void factAdded(final AgentEvent ae) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								shopList.add(ae.getValue());
								shopModel.fireTableDataChanged();
							}
						});
					}
				});
				return IFuture.DONE;
			}
		});
		shopPanel.add(BorderLayout.CENTER, new JScrollPane(shopTable));

		// 商品信息编辑
		JPanel editPanel = new JPanel();
		final JTextField nameFieldt = new JTextField(8);
		editPanel.add(new JLabel("Sekected item-->Name:"));
		editPanel.add(nameFieldt);
		final JTextField priceField = new JTextField(7);
		editPanel.add(new JLabel("Price:"));
		editPanel.add(priceField);
		final JTextField quantityField = new JTextField(7);
		editPanel.add(new JLabel("Quantity:"));
		editPanel.add(quantityField);
		// 商品列表选中监听
		shopTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int sel = shopTable.getSelectedRow();
				if (sel != -1) {
					nameFieldt.setText((String) shopModel.getValueAt(sel, 0));
					priceField.setText(String.valueOf(shopModel.getValueAt(sel, 1)));
					quantityField.setText(String.valueOf(shopModel.getValueAt(sel, 2)));
				}
			}
		});

		// 编辑或添加按钮
		JButton edit = new JButton("Edit/Add");
		edit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int sel = shopTable.getSelectedRow();
				final String name;
				// 当选中商品时编辑，未选中为新增
				if (sel != -1) {
					name = (String) shopModel.getValueAt(sel, 0);
				} else {
					name = "";
				}
				final String ename = (String) nameFieldt.getText();
				final double price = Double.parseDouble(priceField.getText());
				final int quantity = Integer.parseInt(quantityField.getText());

				// 异步目标执行
				agent.scheduleStep(new IComponentStep<Void>() {
					@Override
					@Classname("edit")
					public IFuture<Void> execute(IInternalAccess ia) {
						IBDIInternalAccess bia = (IBDIInternalAccess) ia;
						// 目标创建
						final IGoal edit = bia.getGoalbase().createGoal("edit");
						edit.getParameter("name").setValue(name);
						edit.getParameter("ename").setValue(ename);
						edit.getParameter("price").setValue(price);
						edit.getParameter("quantity").setValue(quantity);
						// 添加目标监听
						edit.addGoalListener(new IGoalListener() {
							@Override
							public void goalFinished(AgentEvent ae) {
								// 按钮点击后，编辑域置空
								nameFieldt.setText("");
								priceField.setText("");
								quantityField.setText("");
								// 编辑失败处理
								if (!edit.isSucceeded()) {
									final String text = SUtil
											.wrapText("Item edit Failed. " + edit.getException().getMessage());
									SwingUtilities.invokeLater(new Runnable() {
										public void run() {
											JOptionPane.showMessageDialog(SGUI.getWindowParent(ShopPanel.this), text,
													"Edit problem", JOptionPane.INFORMATION_MESSAGE);
										}
									});
								}
							}

							@Override
							public void goalAdded(AgentEvent ae) {
							}
						});
						// 目标优先级调整
						bia.getGoalbase().dispatchTopLevelGoal(edit);
						return IFuture.DONE;
					}
				});
			}
		});

		// 删除按钮
		JButton remove = new JButton("Remove");
		remove.addActionListener(new ActionListener() {
			@Override
			@Classname("remove")
			public void actionPerformed(ActionEvent e) {
				int sel = shopTable.getSelectedRow();
				if (sel != -1) {// 必须选中行
					final String name = (String) shopModel.getValueAt(sel, 0);

					agent.scheduleStep(new IComponentStep<Void>() {
						@Override
						public IFuture<Void> execute(IInternalAccess ia) {
							IBDIInternalAccess bia = (IBDIInternalAccess) ia;
							final IGoal edit = bia.getGoalbase().createGoal("edit");
							edit.getParameter("name").setValue(name);
							edit.addGoalListener(new IGoalListener() {

								@Override
								public void goalFinished(AgentEvent ae) {
									if (!edit.isSucceeded()) {
										final String text = SUtil
												.wrapText("Item remove Failed. " + edit.getException().getMessage());
										SwingUtilities.invokeLater(new Runnable() {
											public void run() {
												JOptionPane.showMessageDialog(SGUI.getWindowParent(ShopPanel.this),
														text, "Remove problem", JOptionPane.INFORMATION_MESSAGE);
											}
										});
									}
								}

								@Override
								public void goalAdded(AgentEvent ae) {
								}
							});
							bia.getGoalbase().dispatchTopLevelGoal(edit);
							return IFuture.DONE;
						}
					});
				}
			}
		});
		editPanel.add(edit);
		editPanel.add(remove);

		setLayout(new VerticalLayout());
		add(namePanel);
		add(shopPanel);
		add(editPanel);
	}

}
