package jadex.bdi.jqs7.shop;

import jadex.bdi.jqs7.shop.IShopService;
import jadex.bdi.jqs7.shop.ItemInfo;
import jadex.bdi.jqs7.shop.ItemTableModel;
import jadex.bdi.runtime.AgentEvent;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bdi.runtime.IBeliefListener;
import jadex.bdi.runtime.IBeliefSetListener;
import jadex.bdi.runtime.IGoal;
import jadex.bdi.runtime.IGoalListener;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.commons.SUtil;
import jadex.commons.future.DelegationResultListener;
import jadex.commons.future.Future;
import jadex.commons.future.IFuture;
import jadex.commons.gui.SGUI;
import jadex.commons.gui.future.SwingDefaultResultListener;
import jadex.commons.transformation.annotations.Classname;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

/**
 * Customer gui that allows buying items at different shops.
 */
public class CustomerPanel extends JPanel {
	// -------- attributes --------

	/**
	 * 
	 */
	private static final long serialVersionUID = -5850821825602940027L;
	protected IBDIExternalAccess agent;
	protected List shoplist = new ArrayList();
	protected JCheckBox remote;// 远程搜索复选框
	protected JTable shoptable;// 商品项目列表
	protected AbstractTableModel shopmodel = new ItemTableModel(shoplist);

	protected List invlist = new ArrayList();// 客户库存
	protected AbstractTableModel invmodel = new ItemTableModel(invlist);
	protected JTable invtable;// 客户库存列表
	protected Map shops;

	// -------- constructors --------

	/**
	 * Create a new gui.
	 */
	public CustomerPanel(final IBDIExternalAccess agent) {
		this.agent = agent;
		this.shops = new HashMap();

		// 商店名下拉框
		final JComboBox shopscombo = new JComboBox();
		shopscombo.addItem("none");
		// 项目添加监听器
		shopscombo.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				if (shops.get(shopscombo.getSelectedItem()) instanceof IShopService) {
					refresh((IShopService) shops.get(shopscombo.getSelectedItem()));
				}
			}
		});

		remote = new JCheckBox("Remote");
		remote.setToolTipText("Also search remote platforms for shops.");
		// 搜索按钮
		final JButton searchbut = new JButton("Search");
		// 搜索按钮监听器
		searchbut.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchbut.setEnabled(false);

				// SServiceProvider.getServices(agent.getServiceProvider(),
				// IShop.class, remote.isSelected(), true)
				// 智能体与界面同步调度器
				IFuture<Collection<IShopService>> ret = agent
						.scheduleStep(new IComponentStep<Collection<IShopService>>() {
					public IFuture<Collection<IShopService>> execute(IInternalAccess ia) {
						// 服务通道创建
						Future<Collection<IShopService>> ret = new Future<Collection<IShopService>>();
						if (remote.isSelected()) { // 启用远程搜索
							// 服务获取
							IFuture<Collection<IShopService>> fut = ia.getServiceContainer()
									.getRequiredServices("remoteshopservices");
							// 添加服务结果监听器
							fut.addResultListener(new DelegationResultListener<Collection<IShopService>>(ret));
						} else {// 为启用远程搜索
							IFuture<Collection<IShopService>> fut = ia.getServiceContainer()
									.getRequiredServices("localshopservices");
							fut.addResultListener(new DelegationResultListener<Collection<IShopService>>(ret));
						}
						return ret;
					}
				});

				// 监听服务返回的结果
				ret.addResultListener(new SwingDefaultResultListener<Collection<IShopService>>(CustomerPanel.this) {
					public void customResultAvailable(Collection<IShopService> coll) {
						searchbut.setEnabled(true);
						// System.out.println("Customer search result:
						// "+result);
						// 移除所有下拉框元素(刷新)
						((DefaultComboBoxModel) shopscombo.getModel()).removeAllElements();
						shops.clear();
						// 对搜索结果判空
						if (coll != null && coll.size() > 0) {
							// 使用迭代器将搜索结果添加到下拉框
							for (Iterator<IShopService> it = coll.iterator(); it.hasNext();) {
								IShopService shop = it.next();
								shops.put(shop.getName(), shop);
								((DefaultComboBoxModel) shopscombo.getModel()).addElement(shop.getName());
							}
						} else {
							((DefaultComboBoxModel) shopscombo.getModel()).addElement("none");
						}
					}

					// 服务异常处理
					public void customExceptionOccurred(Exception exception) {
						searchbut.setEnabled(true);
						super.customExceptionOccurred(exception);
					}
				});
			}
		});

		// 数字格式器，格式为显示小数点后两位
		final NumberFormat df = NumberFormat.getInstance();
		df.setMaximumFractionDigits(2);
		df.setMinimumFractionDigits(2);

		// 金钱输入框
		final JTextField money = new JTextField(5);
		// 智能体同步调度器
		agent.scheduleStep(new IComponentStep<Void>() {
			@Classname("initialMoney")
			public IFuture<Void> execute(IInternalAccess ia) {
				IBDIInternalAccess bia = (IBDIInternalAccess) ia;
				// 获取用户金钱
				final Object mon = bia.getBeliefbase().getBelief("money").getFact();
				SwingUtilities.invokeLater(new Runnable() {
					// 金钱设置
					public void run() {
						money.setText(df.format(mon));
					}
				});
				return IFuture.DONE;
			}
		});
		money.setEditable(false);

		// 智能体同步调度器
		agent.scheduleStep(new IComponentStep<Void>() {
			@Classname("money")
			public IFuture<Void> execute(IInternalAccess ia) {
				IBDIInternalAccess bia = (IBDIInternalAccess) ia;
				// 金钱信念监听器
				bia.getBeliefbase().getBelief("money").addBeliefListener(new IBeliefListener() {
					public void beliefChanged(final AgentEvent ae) {
						SwingUtilities.invokeLater(new Runnable() {
							// 金钱数值发生变化时重新设置金钱输入框的值
							public void run() {
								money.setText(df.format(ae.getValue()));
							}
						});
					}
				});
				return IFuture.DONE;
			}
		});

		// 创建销售面板
		JPanel selpanel = new JPanel(new GridBagLayout());
		selpanel.setBorder(new TitledBorder(new EtchedBorder(), "Properties"));
		int x = 0;
		int y = 0;
		selpanel.add(new JLabel("Money: "), new GridBagConstraints(x, y, 1, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
		x++;
		selpanel.add(money, new GridBagConstraints(x, y, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));
		x++;
		selpanel.add(new JLabel("Available shops: "), new GridBagConstraints(x, y, 1, 1, 0, 0, GridBagConstraints.EAST,
				GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
		x++;
		selpanel.add(shopscombo, new GridBagConstraints(x, y, 1, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0));
		x++;
		selpanel.add(searchbut, new GridBagConstraints(x, y, 1, 1, 0, 0, GridBagConstraints.WEST,
				GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0));
		x++;
		selpanel.add(remote, new GridBagConstraints(x, y, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE,
				new Insets(2, 2, 2, 2), 0, 0));

		// 创建商店面板
		JPanel shoppanel = new JPanel(new BorderLayout());
		shoppanel.setBorder(new TitledBorder(new EtchedBorder(), "Shop Catalog"));
		shoptable = new JTable(shopmodel);
		// 设置表格可视区域
		shoptable.setPreferredScrollableViewportSize(new Dimension(600, 120));
		// 设置单行可选
		shoptable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// 设置可滚动
		shoppanel.add(BorderLayout.CENTER, new JScrollPane(shoptable));

		// 创建客户库存面板
		JPanel invpanel = new JPanel(new BorderLayout());
		invpanel.setBorder(new TitledBorder(new EtchedBorder(), "Customer Inventory"));
		invtable = new JTable(invmodel);
		invtable.setPreferredScrollableViewportSize(new Dimension(600, 120));
		invpanel.add(BorderLayout.CENTER, new JScrollPane(invtable));

		// 库存同步调度器
		agent.scheduleStep(new IComponentStep<Void>() {
			@Classname("inventory")
			public IFuture<Void> execute(IInternalAccess ia) {
				IBDIInternalAccess bia = (IBDIInternalAccess) ia;
				try {
					bia.getBeliefbase().getBeliefSet("inventory").addBeliefSetListener(new IBeliefSetListener() {
						// 信念事实被移除
						public void factRemoved(final AgentEvent ae) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									invlist.remove(ae.getValue());
									invmodel.fireTableDataChanged();
								}
							});
						}

						// 修改
						public void factChanged(final AgentEvent ae) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									invlist.remove(ae.getValue());
									invlist.add(ae.getValue());
									invmodel.fireTableDataChanged();
								}
							});
						}

						// 新增
						public void factAdded(final AgentEvent ae) {
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									invlist.add(ae.getValue());
									invmodel.fireTableDataChanged();
								}
							});
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				return IFuture.DONE;
			}
		});

		// 按钮面板
		JPanel butpanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		// butpanel.setBorder(new TitledBorder(new EtchedBorder(), "Actions"));
		// 购买按钮
		JButton buy = new JButton("Buy");
		final JTextField item = new JTextField(8);
		item.setEditable(false);
		butpanel.add(new JLabel("Selected item:"));
		butpanel.add(item);
		butpanel.add(buy);
		// 购买按钮监听器
		buy.addActionListener(new ActionListener() {
			// 按钮被按下
			public void actionPerformed(ActionEvent e) {
				// 获取商店中被选中的那一行
				int sel = shoptable.getSelectedRow();
				if (sel != -1) {// 行号合法判断
					// 获取该行商品名称与价格
					final String name = (String) shopmodel.getValueAt(sel, 0);
					final Double price = (Double) shopmodel.getValueAt(sel, 1);
					// 获取下拉框的商店名
					final IShopService shop = (IShopService) shops.get(shopscombo.getSelectedItem());
					// 购买行为同步调度器
					agent.scheduleStep(new IComponentStep<Void>() {
						@Classname("buy")
						public IFuture<Void> execute(IInternalAccess ia) {
							IBDIInternalAccess bia = (IBDIInternalAccess) ia;
							// 获取购买行为目标
							final IGoal buy = bia.getGoalbase().createGoal("buy");
							// 目标参数设置
							buy.getParameter("name").setValue(name);
							buy.getParameter("shop").setValue(shop);
							buy.getParameter("price").setValue(price);
							// 目标监听器
							buy.addGoalListener(new IGoalListener() {
								public void goalFinished(AgentEvent ae) {
									// Update number of available items
									refresh(shop);
									// 购买失败处理
									if (!buy.isSucceeded()) {
										final String text = SUtil.wrapText(
												"Item could not be bought. " + buy.getException().getMessage());
										SwingUtilities.invokeLater(new Runnable() {
											public void run() {
												JOptionPane.showMessageDialog(SGUI.getWindowParent(CustomerPanel.this),
														text, "Buy problem", JOptionPane.INFORMATION_MESSAGE);
											}
										});
									}
								}

								public void goalAdded(AgentEvent ae) {
								}
							});
							bia.getGoalbase().dispatchTopLevelGoal(buy);
							return IFuture.DONE;
						}
					});
				}
			}
		});

		shoptable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			public void valueChanged(ListSelectionEvent e) {
				int sel = shoptable.getSelectedRow();
				if (sel != -1) {
					item.setText("" + shopmodel.getValueAt(sel, 0));
				}
			}

		});

		setLayout(new GridBagLayout());
		x = 0;
		y = 0;
		add(selpanel, new GridBagConstraints(x, y++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(2, 2, 2, 2), 0, 0));
		add(shoppanel, new GridBagConstraints(x, y++, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(2, 2, 2, 2), 0, 0));
		add(invpanel, new GridBagConstraints(x, y++, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(2, 2, 2, 2), 0, 0));
		add(butpanel, new GridBagConstraints(x, y++, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH,
				new Insets(2, 2, 2, 2), 0, 0));

		// refresh();
	}

	/**
	 * Create a customer gui frame. / public static void createCustomerGui(final
	 * IBDIExternalAccess agent) { final JFrame f = new JFrame(); f.add(new
	 * CustomerPanel(agent)); f.pack();
	 * f.setLocation(SGUI.calculateMiddlePosition(f)); f.setVisible(true);
	 * f.addWindowListener(new WindowAdapter() { public void
	 * windowClosing(WindowEvent e) { agent.killAgent(); } });
	 * agent.addAgentListener(new IAgentListener() { public void
	 * agentTerminating(AgentEvent ae) { f.setVisible(false); f.dispose(); }
	 * 
	 * public void agentTerminated(AgentEvent ae) { } }); }
	 */

	/**
	 * Method to be called when goals may have changed.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void refresh(IShopService shop) {
		if (shop != null) {
			shop.getCatalog().addResultListener(new SwingDefaultResultListener(CustomerPanel.this) {
				public void customResultAvailable(Object result) {
					int sel = shoptable.getSelectedRow();
					ItemInfo[] aitems = (ItemInfo[]) result;
					shoplist.clear();
					for (int i = 0; i < aitems.length; i++) {
						if (!shoplist.contains(aitems[i])) {
							// System.out.println("added: "+aitems[i]);
							shoplist.add(aitems[i]);
						}
					}
					shopmodel.fireTableDataChanged();
					if (sel != -1 && sel < aitems.length)
						((DefaultListSelectionModel) shoptable.getSelectionModel()).setSelectionInterval(sel, sel);
				}
			});
		} else {
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					shoplist.clear();
					shopmodel.fireTableDataChanged();
				}
			});
		}
	}

}
