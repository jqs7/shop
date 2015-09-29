package jadex.bdi.jqs7.shop;

import jadex.bdi.jqs7.shop.CustomerPanel;
import jadex.bdi.runtime.IBDIExternalAccess;
import jadex.bdi.runtime.IBDIInternalAccess;
import jadex.bridge.IComponentStep;
import jadex.bridge.IInternalAccess;
import jadex.bridge.service.types.monitoring.IMonitoringEvent;
import jadex.bridge.service.types.monitoring.IMonitoringService.PublishEventLevel;
import jadex.commons.future.IFuture;
import jadex.commons.future.IResultListener;
import jadex.commons.future.IntermediateDefaultResultListener;
import jadex.commons.gui.SGUI;
import jadex.commons.gui.future.SwingIntermediateResultListener;
import jadex.commons.transformation.annotations.Classname;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

/**
 * Frame for displaying of the customer gui.
 */
public class CustomerFrame extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3685065838092822757L;

	/**
	 * Create a new frame.
	 */
	public CustomerFrame(final IBDIExternalAccess agent) {
		super(agent.getComponentIdentifier().getName());

		// 新增客户界面
		add(new CustomerPanel(agent));
		// 界面激活
		pack();
		// 界面位置设置
		setLocation(SGUI.calculateMiddlePosition(this));
		// 设置为可视
		setVisible(true);
		// 添加窗口监听器
		addWindowListener(new WindowAdapter() {
			// 接收到窗口关闭事件
			public void windowClosing(WindowEvent e) {
				// agent.killAgent();
				agent.killComponent();
			}
		});

		// Dispose frame on exception.
		// 异常捕获处理监听器
		IResultListener<Void> dislis = new IResultListener<Void>() {
			// 异常结果处理
			public void exceptionOccurred(Exception exception) {
				dispose();
			}

			// 正常结果处理
			public void resultAvailable(Void result) {
			}
		};

		// 智能体的步调度器，实现智能体与外部构件的数据同步，并加入异常捕获监听器
		agent.scheduleStep(new IComponentStep<Void>() {
			@Classname("dispose")
			public IFuture<Void> execute(IInternalAccess ia) {
				IBDIInternalAccess bia = (IBDIInternalAccess) ia;
				// bia.addComponentListener(new TerminationAdapter()
				// {
				// public void componentTerminated()
				// {
				// SwingUtilities.invokeLater(new Runnable()
				// {
				// public void run()
				// {
				// setVisible(false);
				// dispose();
				// }
				// });
				// }
				// });

				bia.subscribeToEvents(IMonitoringEvent.TERMINATION_FILTER, false, PublishEventLevel.COARSE)
						.addResultListener(new SwingIntermediateResultListener<IMonitoringEvent>(
								new IntermediateDefaultResultListener<IMonitoringEvent>() {
					public void intermediateResultAvailable(IMonitoringEvent result) {
						setVisible(false);
						dispose();
					}
				}));
				return IFuture.DONE;
			}
		}).addResultListener(dislis);
	}
}
