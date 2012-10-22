import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

public class GraphGui extends ApplicationFrame{

	private static class JdbcPanel extends JPanel 
	implements AdjustmentListener, ChangeListener	{

		private static final int step = 10;
		private ValueAxis domainAxis;
		private XYDataset xydataset;
		private static int preferedHeight = 500;
		private static int preferedWidth = 270;


		private XYDataset createPegelAndelfingen(){
			YIntervalSeriesCollection timeseriescollection = new YIntervalSeriesCollection();
			JdbcYIntervalSeries timeseries = new JdbcYIntervalSeries("Pegel Andelfingen2",
					"jdbc:mysql://localhost:3306/group08",
					"com.mysql.jdbc.Driver",
					"ads", 
					"password",
					"timed",
					"PEGEL",
					"dataset_1",
					null);
			Map<Integer,Integer> levels = new HashMap<Integer,Integer>();
			levels.put(2,      500000);
			levels.put(4,     1000000);
			levels.put(8,     2500000);
			levels.put(16,    5000000);
			levels.put(32,   10000000);
			levels.put(64,   50000000);
			levels.put(128, 100000000);
			levels.put(256,1000000000);
			timeseries.setUpAggregation(levels);
			Range range = timeseries.getDomainRange();
			timeseries.update((new Double(range.getLowerBound())).longValue(), (new Double(range.getLength())).longValue());
			timeseriescollection.addSeries(timeseries);
			return timeseriescollection;
		}


		private JFreeChart createChart()
		{
			xydataset = createPegelAndelfingen();
			JFreeChart jfreechart = ChartFactory.createTimeSeriesChart("Time", "Date", "Pegel", xydataset, true, true, false);

			XYPlot xyplot = jfreechart.getXYPlot();
			domainAxis = xyplot.getDomainAxis();
			domainAxis.setLowerMargin(0.0D);
			domainAxis.setUpperMargin(0.0D);
			YIntervalSeries series = ((YIntervalSeriesCollection) xydataset).getSeries(0);
			domainAxis.setRange(Math.floor(series.getX(0).doubleValue()), Math.ceil(series.getX(series.getItemCount()-1).doubleValue()));
			rangeAxis = new NumberAxis("Pegel2");
			DeviationRenderer deviationrenderer = new DeviationRenderer(true, false);
			deviationrenderer.setSeriesStroke(0, new BasicStroke(3F, 1, 1));
			deviationrenderer.setSeriesStroke(0, new BasicStroke(3F, 1, 1));
			deviationrenderer.setSeriesFillPaint(0, new Color(255, 200, 200));
			deviationrenderer.setBaseShapesVisible(true);
			deviationrenderer.setSeriesShapesFilled(0, Boolean.FALSE);
			deviationrenderer.setSeriesShapesFilled(1, Boolean.FALSE);
			xyplot.setRenderer(deviationrenderer);			
			xyplot.setRangeAxis(rangeAxis);
			rangeAxis.setRange(350, 360);
			return jfreechart;
		}

		private JScrollBar scrollbar;
		private NumberAxis rangeAxis;
		private double factorRange;
		private double factorDomain;
		private double extentR;
		private long extentD;
		private JScrollBar scrollbarh;
		private double valueR;
		private long valueD;
		private JSlider quantileSlider;
		private int quantile=0;
		private JTextField textField;

		public JdbcPanel()
		{
			super(new BorderLayout());
			//			degrees = 45D;
			//			JPanel jpanel = new JPanel(new GridLayout(3, 1));
			JPanel jpanel = new JPanel();
			jpanel.setLayout(new BoxLayout(jpanel, BoxLayout.Y_AXIS));

			JFreeChart jfreechart = createChart();
			ChartPanel chartpanel = new ChartPanel(jfreechart);
			chartpanel.setPreferredSize(new Dimension(preferedHeight, preferedWidth));
			Range r = rangeAxis.getRange();
			factorRange = 80.0/r.getLength();
			double diff = r.getLength()*0.2;
			scrollbar = new JScrollBar(JScrollBar.VERTICAL, (int)Math.floor(r.getLowerBound()*factorRange) , 
					(int) Math.ceil(r.getLength()*factorRange), 
					(int) Math.floor(((r.getLowerBound()-diff)*factorRange)), 
					(int) Math.ceil(((r.getUpperBound()+diff)*factorRange)));
			scrollbar.addAdjustmentListener(this);

			r = domainAxis.getRange();
			valueD = (long) r.getLowerBound();
			extentD = (long) r.getLength();
			factorDomain = 80.0/extentD;
			diff = extentD*0.2;
			scrollbarh = new JScrollBar(JScrollBar.HORIZONTAL, 
					(int)Math.floor(valueD*factorDomain) , 
					(int) Math.ceil(extentD*factorDomain), 
					(int) Math.floor(((valueD-diff)*factorDomain)), 
					(int) Math.ceil(((valueD+extentD+diff)*factorDomain)));
			scrollbarh.addAdjustmentListener(this);

			quantileSlider = new JSlider(JSlider.VERTICAL, 0, 5, quantile);
			quantileSlider.setPaintLabels(true);
			quantileSlider.setPaintTicks(true);
			quantileSlider.setMajorTickSpacing(1);
			//quantileSlider.setMinorTickSpacing(5);
			quantileSlider.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			quantileSlider.setSnapToTicks(true);
			quantileSlider.setInverted(true);
			Hashtable<Integer, JLabel> table = new Hashtable<Integer, JLabel>();
			table.put (0, new JLabel("0"));
			table.put (1, new JLabel("10"));
			table.put (2, new JLabel("20"));
			table.put (3, new JLabel("30"));
			table.put (4, new JLabel("40"));
			table.put (5, new JLabel("50"));
			quantileSlider.setLabelTable (table);
			quantileSlider.addChangeListener(this);


			jpanel.add(scrollbar);
			add(jpanel, "West");
			add(chartpanel);
//			add(quantileSlider,"East");
			add(scrollbarh,"South");
//			JPanel jpanel2 = new JPanel();
//			jpanel2.setLayout(new BoxLayout(jpanel2, BoxLayout.Y_AXIS));
//			jpanel2.add(scrollbarh);
//			JPanel jpanel21 = new JPanel();
//			jpanel21.add(new JLabel("scroll factor"));
//			JButton b = new JButton("/2");
//			b.addActionListener(this);
//			jpanel21.add(b);
//			b = new JButton("*2");
//			b.addActionListener(this);
//			jpanel21.add(b);
//			textField = new JTextField(20);
//			textField.addActionListener(this);
//			jpanel21.add(textField);
//			jpanel2.add(jpanel21);
//			add(jpanel2,"South");

			//add(new JButton("+"),"SouthEast");

		}

		public void adjustmentValueChanged(AdjustmentEvent e) {
			if(e.getSource() == scrollbar)
			{
				valueR = scrollbar.getValue()/factorRange;
				extentR = scrollbar.getVisibleAmount()/factorRange;
				rangeAxis.setRange(valueR, valueR+extentR);
			} else if(e.getSource() == scrollbarh)
			{
				valueD = (long) (scrollbarh.getValue()/factorDomain);
				extentD = (long) (scrollbarh.getVisibleAmount()/factorDomain);
				// reload data set
				YIntervalSeriesCollection col = (YIntervalSeriesCollection) xydataset;
				for(int i=0; i<col.getSeriesCount(); i++){
					JdbcYIntervalSeries series = (JdbcYIntervalSeries) col.getSeries(i);
					series.update(valueD, extentD);
				}
				domainAxis.setRange(valueD, valueD+extentD);
			} 
		}

		public void stateChanged(ChangeEvent changeevent) {
			if(changeevent.getSource() == quantileSlider)
			{
				quantile = quantileSlider.getValue();
				YIntervalSeriesCollection col = (YIntervalSeriesCollection) xydataset;
				for(int i=0; i<col.getSeriesCount(); i++){
					JdbcYIntervalSeries series = (JdbcYIntervalSeries) col.getSeries(i);
					series.update(valueD, extentD);
				}
			} 
		}

	} // end of JdbcPanel class


	public GraphGui(String title){
		super(title);
		setContentPane(new JdbcPanel());

	}

	public static void main(String args[])
	{
		UIManager.put("ScrollBarUI", GraphGuiScrollBarUI.class.getName());

		GraphGui test = new GraphGui("GraphGUI demo");
		test.pack();
		RefineryUtilities.centerFrameOnScreen(test);
		test.setVisible(true);
	}
}
