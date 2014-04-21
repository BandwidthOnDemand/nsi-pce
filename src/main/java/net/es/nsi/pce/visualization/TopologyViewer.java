package net.es.nsi.pce.visualization;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractModalGraphMouse;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.BasicVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JMenuBar;
import javax.swing.JRadioButton;
import net.es.nsi.pce.spring.SpringContext;
import net.es.nsi.pce.topology.provider.TopologyProvider;
import org.apache.commons.collections15.functors.ChainedTransformer;
import org.apache.log4j.xml.DOMConfigurator;
import org.springframework.context.ApplicationContext;


import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;
import net.es.nsi.pce.topology.model.NsiStpFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple NSI/NML topology viewer with shortest path computation.
 * 
 * @author hacksaw
 */
public class TopologyViewer extends JPanel {
	private static final long serialVersionUID = 7526217664458188502L;
    
    private final Logger log;
    

    // VLAN menu pull down default.
    private static final String UNSET = "unset";
    
	//Starting vertex as specified in pull down selector.
	private NetworkVertex mSourceNetwork;

	//Ending vertex as specified in pull down selector.
	private NetworkVertex mDestinationNetwork;
    
    // VLAN id for path finding as specified in pull down selector.
    private int mVlanId = -1;
    
    // Source STP in the context of a network.
    private String mSourceSTP = null;
    
    // Destination STP in the context of mTo network.
    private String mDestinationSTP = null;
    
    private HashMap<String, NetworkVertex> networkVerticies;
            
    // The Graph model displayed.
	private Graph<NetworkVertex, SdpType> mGraph;
    
    // The list of verticies computed to be on the shortest path.
	private Set<NetworkVertex> mPred = new HashSet<>();
    
    // The list of edges computed to be on the shortest path.
    private Set<SdpType> mEdge = new HashSet<>();
    
    // The NSI topology used to build the graph.
	private NsiTopology nsiTopology;
    
    private ScalingControl scaler = new CrossoverScalingControl();

    private JPanel sourceStpPanel = new JPanel();
    private JPanel destinationStpPanel = new JPanel();
    
    /**
     * 
     * @throws Exception 
     */
    @SuppressWarnings("unchecked")
	public TopologyViewer(String configDir) throws Exception {
        // Configure path to log4j configuration.
        String log4jConfig = new StringBuilder(configDir).append("log4j-viewer.xml").toString().replace("/", File.separator);
        String beanConfig = new StringBuilder(configDir).append("beans.xml").toString().replace("/", File.separator);
        
        // Load and watch the log4j configuration file for changes.
        DOMConfigurator.configureAndWatch(log4jConfig, 45 * 1000);
        
        log = LoggerFactory.getLogger(getClass());
        
        // Get a reference to the topology provider through spring.
        SpringContext sc = SpringContext.getInstance();
        ApplicationContext context = sc.initContext(beanConfig);
        TopologyProvider provider = (TopologyProvider) context.getBean("topologyProvider");
        
        // Load NSI topology.
        try {
            provider.loadTopology();
        }
        catch (Exception ex) {
            log.error("loadTopology() failed", ex);
            throw ex;
        }
        
        nsiTopology = provider.getTopology();
        
        // Populate a local NetworkVertex view extending the NSI Network object.
        networkVerticies = new HashMap<>();
        for (NetworkType network : nsiTopology.getNetworks()) {
            log.debug("Adding vertex " + network.getId());
            networkVerticies.put(network.getId(), new NetworkVertex(network));
        }    
        
		this.mGraph = getGraph();
        
        Dimension layoutSize = new Dimension(Display.maxX,Display.maxY);

        final Layout<NetworkVertex, SdpType> layout = new StaticLayout<>(mGraph,
        		new ChainedTransformer(new Transformer[]{
        				new NetworkTransformer(),
        				new XYPixelTransformer(layoutSize)
        		}));
        
        layout.setSize(layoutSize);
        
        // The visual component and renderer for the graph
        final VisualizationViewer<NetworkVertex, SdpType> vv =  new VisualizationViewer<>(layout, layoutSize);
        
        vv.getRenderContext().setVertexDrawPaintTransformer(new NetworkVertexDrawPaintFunction<NetworkVertex>());
        vv.getRenderContext().setVertexFillPaintTransformer(new NetworkVertexFillPaintFunction<NetworkVertex>());
        vv.getRenderContext().setEdgeDrawPaintTransformer(new SdpEdgePaintFunction());
        vv.getRenderContext().setEdgeStrokeTransformer(new SdpEdgeStrokeFunction());
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<NetworkVertex>());
        vv.setGraphMouse(new DefaultModalGraphMouse<NetworkVertex, SdpType>());
        vv.addGraphMouseListener(new TestGraphMouseListener<NetworkVertex>());
        vv.setBackground(Color.WHITE);
        
        vv.addPreRenderPaintable(new VisualizationViewer.Paintable(){
            @Override
            public void paint(Graphics g) {
                Graphics2D g2d = (Graphics2D)g;
                AffineTransform oldXform = g2d.getTransform();
                AffineTransform lat = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).getTransform();
                AffineTransform vat = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).getTransform();
                AffineTransform at = new AffineTransform();
                at.concatenate(g2d.getTransform());
                at.concatenate(vat);
                at.concatenate(lat);
                g2d.setTransform(at);
                g2d.setTransform(oldXform);
                
                if(mPred == null) return;
                
                // For all edges, paint edges that are in shortest path
                for (SdpType e : layout.getGraph().getEdges()) {
                    
                    if(isOnShortestPath(e)) {
                        NetworkVertex v1 = mGraph.getEndpoints(e).getFirst();
                        NetworkVertex v2 = mGraph.getEndpoints(e).getSecond();
                        Point2D p1 = layout.transform(v1);
                        Point2D p2 = layout.transform(v2);
                        vv.getRenderContext().getMultiLayerTransformer().transform(Layer.LAYOUT, p1);
                        vv.getRenderContext().getMultiLayerTransformer().transform(Layer.LAYOUT, p2);
                        Renderer<NetworkVertex, SdpType> renderer = vv.getRenderer();
                        renderer.renderEdge(vv.getRenderContext(), layout, e);
                    }
                }
            }
            
            @Override
            public boolean useTransform() {
                return true;
            }

            });
                
        //vv.getRenderer().setVertexRenderer(new GradientVertexRenderer<Network, Sdp>(
        //				Color.white, Color.red, Color.white, Color.blue,
        //				vv.getPickedVertexState(), false));
 
        // add my listeners for ToolTips
        vv.setVertexToolTipTransformer(new ToStringLabeller());
        vv.setEdgeToolTipTransformer(new Transformer<SdpType, String>() {
            @Override
			public String transform(SdpType edge) {
				return "E" + mGraph.getEndpoints(edge).toString();
			}
        });
        
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        vv.getRenderer().getVertexLabelRenderer().setPositioner(new BasicVertexLabelRenderer.InsidePositioner());
        
        // Labels should show up on lower left of the vertex.
        vv.getRenderer().getVertexLabelRenderer().setPosition(Renderer.VertexLabel.Position.SE);
        
        // A nice white background colour in main map window.
        vv.setBackground(Display.backgroundColor);

        final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
        final AbstractModalGraphMouse graphMouse = new DefaultModalGraphMouse<Number, Number>();
        vv.setGraphMouse(graphMouse);
        vv.addKeyListener(graphMouse.getModeKeyListener());

        JMenuBar menubar = new JMenuBar();
        menubar.add(graphMouse.getModeMenu());
        panel.setCorner(menubar); 
        
        add(panel);
        
        setLayout(new BorderLayout());
        add(vv, BorderLayout.CENTER);
        // set up controls
        add(setUpControls(vv), BorderLayout.SOUTH);
	}

    /**
     * Determine if the link should be displayed based on the results of our
     * shortest path computation.
     * 
     * @param e The STP edge to test for membership in the shortest path.
     * @return True if the edge is in the shortest path, and false otherwise.
     */
    boolean isOnShortestPath(SdpType e) {
        // If there are not edges then there is no this one can be in the
        // shortest path.
        if (mEdge == null || mEdge.isEmpty()) {
            return false;
        }
        
        // Not on shortest path if not in edge list.
        if (!mEdge.contains(e)) {
            return false;
        }
        
        return true;
    }
    
	/**
     * Determines the colour to paint an SDP based on whether it is a member of
     * the shortest path.  
     */
	public class SdpEdgePaintFunction implements Transformer<SdpType, Paint> {
	    
        @Override
		public Paint transform(SdpType e) {
            // Filter first based on vlan selection.
            StpType stpA = nsiTopology.getStp(e.getDemarcationA().getStp().getId());
            int vlanId = NsiStpFactory.getVlanId(stpA);
            
            if (mVlanId != -1 && mVlanId != vlanId) {
                return Display.backgroundColor;
            }
            
            // When there are no verticies selected we paint the lines black.
			if ( mPred == null || mPred.isEmpty()) {
                return Color.BLACK;
            }
            
            // If the edge is part of the shortest path we colour it blue.
			if (isOnShortestPath(e)) {
				return new Color(0.0f, 0.0f, 1.0f, 0.5f); //Color.BLUE;
			}
            else {
                // If it is not part of the shortest path then we set it to gray.
				return Color.LIGHT_GRAY;
			}
		}
	}

    /**
     * Determines the thickness to paint an SDP based on whether it is a member 
     * of the shortest path (thick) or not (thin).  
     */
	public class SdpEdgeStrokeFunction implements Transformer<SdpType, Stroke> {
        protected final Stroke INVISIBLE = new BasicStroke(0);
        protected final Stroke THIN = new BasicStroke(1);
        protected final Stroke THICK = new BasicStroke(5);

        @Override
        public Stroke transform(SdpType e) {
            
            // Filter first based on vlan selection.
            StpType stpA = nsiTopology.getStp(e.getDemarcationA().getStp().getId());
            int vlanId = NsiStpFactory.getVlanId(stpA);
            
            if (mVlanId != -1 && mVlanId != vlanId) {
                return INVISIBLE;
            }
            
            // When there are no verticies selected we paint the lines thin.
			if (mPred == null || mPred.isEmpty()) {
                return THIN;
            }
            
            // If the edge is part of the shortest path we paint it thick.
			if (isOnShortestPath(e) ) {
			    return THICK;
			}
            else {
			    return THIN;
            }
        }
	    
    }
    
    /**
     * Determines the colour to paint outline of a NetworkVertex vertex based on
     * whether it is a member of the shortest path.  
     */
	public class NetworkVertexDrawPaintFunction<NetworkVertex> implements Transformer<NetworkVertex, Paint> {

        @Override
		public Paint transform(NetworkVertex v) {
			return Color.black;
		}

	}

    /**
     * Determines the colour to fill a NetworkVertex vertex based on whether it is
     * a member of the shortest path.  
     */
	public class NetworkVertexFillPaintFunction<NetworkVertex> implements Transformer<NetworkVertex, Paint> {

        @Override
		public Paint transform( NetworkVertex v ) {
            // Source and destination NetworkVertex verticies are filled with blue.
			if ( v == mSourceNetwork) {
				return Color.BLUE;
			}
            
			if ( v == mDestinationNetwork ) {
				return Color.BLUE;
			}
            
            // If there are no verticies then we colour the Network gray.
			if ( mPred == null ) {
				return Color.LIGHT_GRAY;
			}
            else {
                // Networks on the path (but not source or destination) are
                // painted red.
				if (mPred.contains(v)) {
					return Color.RED;
				}
                // Networks not on the path are painted light gray.
                else {
					return Color.LIGHT_GRAY;
				}
			}
		}
	}

    /**
     * Configure the JPanel with our selectable pull downs.
     * 
     * @return The configured JPanel.
     */
	private JPanel setUpControls(final VisualizationViewer<NetworkVertex, SdpType> vv) {
        
        Box controls = Box.createHorizontalBox();

        // The scale selector.
        JButton plus = new JButton("+");
        plus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1.1f, vv.getCenter());
            }
        });
        JButton minus = new JButton("-");
        minus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                scaler.scale(vv, 1/1.1f, vv.getCenter());
            }
        });
        
        JPanel zoomPanel = new JPanel(new GridLayout(0,1));
        zoomPanel.setBorder(BorderFactory.createTitledBorder("Scale"));
        zoomPanel.add(plus);
        zoomPanel.add(minus);

        // The curve edge type selector.
        ButtonGroup radio = new ButtonGroup();
        JRadioButton lineButton = new JRadioButton("Line");
        lineButton.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.Line<NetworkVertex, SdpType>());
                    vv.repaint();
                }
            }
        });
        
        JRadioButton quadButton = new JRadioButton("QuadCurve");
        quadButton.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.QuadCurve<NetworkVertex, SdpType>());
                    vv.repaint();
                }
            }
        });
        
        JRadioButton cubicButton = new JRadioButton("CubicCurve");
        cubicButton.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    vv.getRenderContext().setEdgeShapeTransformer(new EdgeShape.CubicCurve<NetworkVertex, SdpType>());
                    vv.repaint();
                }
            }
        });
        radio.add(lineButton);
        radio.add(quadButton);
        radio.add(cubicButton);
        
        JPanel edgePanel = new JPanel(new GridLayout(0,1));
        edgePanel.setBorder(BorderFactory.createTitledBorder("EdgeType Type"));
        edgePanel.add(lineButton);
        edgePanel.add(quadButton);
        edgePanel.add(cubicButton);
 
        // The Label orientation selector.
        JPanel positionPanel = new JPanel();
        positionPanel.setBorder(BorderFactory.createTitledBorder("Label Position"));
        JComboBox cb = new JComboBox();
        cb.addItem(Renderer.VertexLabel.Position.N);
        cb.addItem(Renderer.VertexLabel.Position.NE);
        cb.addItem(Renderer.VertexLabel.Position.E);
        cb.addItem(Renderer.VertexLabel.Position.SE);
        cb.addItem(Renderer.VertexLabel.Position.S);
        cb.addItem(Renderer.VertexLabel.Position.SW);
        cb.addItem(Renderer.VertexLabel.Position.W);
        cb.addItem(Renderer.VertexLabel.Position.NW);
        cb.addItem(Renderer.VertexLabel.Position.N);
        cb.addItem(Renderer.VertexLabel.Position.CNTR);
        cb.addItem(Renderer.VertexLabel.Position.AUTO);
        cb.addItemListener(new ItemListener() {
            @Override
			public void itemStateChanged(ItemEvent e) {
				Renderer.VertexLabel.Position position = 
					(Renderer.VertexLabel.Position)e.getItem();
				vv.getRenderer().getVertexLabelRenderer().setPosition(position);
				vv.repaint();
			}});
        cb.setSelectedItem(Renderer.VertexLabel.Position.SE);
        positionPanel.add(cb);
        
        // The Source network pull down.
		JPanel sourceNetwork = new JPanel();
		sourceNetwork.add(getNetworkSelectionBox(true));
        
        // The destination network pull down.
		JPanel destinationNetwork = new JPanel();
		destinationNetwork.add(getNetworkSelectionBox(false));

        // The VLAN pulldown.
        JPanel vlan = new JPanel();
		vlan.add(getVlanIdBox());
        
        JPanel pathPanel = new JPanel(new BorderLayout());
        JPanel pathCriteriaPanel = new JPanel(new GridLayout(3,1));
        JPanel pathCriteriaLabelPanel = new JPanel(new GridLayout(3,1));
        JPanel pathTitlePanel = new JPanel(new BorderLayout());
        pathTitlePanel.setBorder(BorderFactory.createTitledBorder("Path Criteria"));
        pathCriteriaPanel.add(sourceNetwork);
        pathCriteriaPanel.add(destinationNetwork);
        pathCriteriaPanel.add(vlan);
        pathCriteriaLabelPanel.add(new JLabel("Source network", JLabel.RIGHT));
        pathCriteriaLabelPanel.add(new JLabel("Destination network", JLabel.RIGHT));
        pathCriteriaLabelPanel.add(new JLabel("VLAN", JLabel.RIGHT));
        pathTitlePanel.add(pathCriteriaLabelPanel, BorderLayout.WEST);
        pathTitlePanel.add(pathCriteriaPanel);
        pathPanel.add(pathTitlePanel);
        
        // The Source STP pull down.
		sourceStpPanel.add(getSourceStpSelectionBox());
        
        // The destination network pull down.
		destinationStpPanel.add(getDestinationStpSelectionBox());
        
        JPanel stpPanel = new JPanel(new BorderLayout());
        JPanel stpCriteriaPanel = new JPanel(new GridLayout(3,1));
        JPanel stpCriteriaLabelPanel = new JPanel(new GridLayout(3,1));
        JPanel stpTitlePanel = new JPanel(new BorderLayout());
        stpTitlePanel.setBorder(BorderFactory.createTitledBorder("STP Criteria"));
        stpCriteriaPanel.add(sourceStpPanel, BorderLayout.WEST);
        stpCriteriaPanel.add(destinationStpPanel, BorderLayout.WEST);
        stpCriteriaLabelPanel.add(new JLabel("Source STP", JLabel.RIGHT));
        stpCriteriaLabelPanel.add(new JLabel("Destination STP", JLabel.RIGHT));
        stpTitlePanel.add(stpCriteriaLabelPanel, BorderLayout.WEST);
        stpTitlePanel.add(stpCriteriaPanel);
        stpPanel.add(stpTitlePanel);

        // Add the component control panels to the frame.
        controls.add(zoomPanel);
        controls.add(edgePanel);
        controls.add(positionPanel);
        controls.add(pathPanel);
        controls.add(stpPanel);

        // This is our primary window.
		JPanel jp = new JPanel(new GridLayout(1,1));
		jp.setBackground(Color.WHITE);
		jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
		jp.setBorder(BorderFactory.createLineBorder(Color.gray, 1));
        jp.add(controls, BorderLayout.SOUTH);

        quadButton.setSelected(true);
        
		return jp;
	}

    /**
     * Populates values for the source and destination network menu pull downs.
     * Also registers a ActionListener to handle selection of a Network from the
     * pull down.
     * 
     * @param source True if this is the source network pull down.
     * 
     * @return The configured menu component.
     */
	private Component getNetworkSelectionBox(final boolean source) {

		Set<String> s = new TreeSet<>();
		
		for (NetworkVertex v : mGraph.getVertices()) {
			s.add(v.getName());
		}
        
        @SuppressWarnings("unchecked")
		final JComboBox choices = new JComboBox(s.toArray());
		choices.setSelectedIndex(-1);
		choices.setBackground(Color.WHITE);
		choices.addActionListener(new ActionListener() {

            @Override
			public void actionPerformed(ActionEvent e) {
				String v = (String) choices.getSelectedItem();
					
				if (source) {
					mSourceNetwork = getNetworkByName(v);
                    
                    sourceStpPanel.removeAll();
                    sourceStpPanel.add(getSourceStpSelectionBox());
                    sourceStpPanel.updateUI();
                    
				} else {
					mDestinationNetwork = getNetworkByName(v);
                    
                    destinationStpPanel.removeAll();
                    destinationStpPanel.add(getDestinationStpSelectionBox());
                    destinationStpPanel.updateUI();
				}
				drawShortest();
				repaint();				
			}
		});
		return choices;
	}

    /**
     * Populate the contents of the VLAN identifier selection pull down and
     * respond to events.
     * 
     * @return The vlanid box to display.
     */
    private Component getVlanIdBox() {

        Set<String> s = new TreeSet<>();
        for (StpType stp : nsiTopology.getStps()) {

            String vlanid = NsiStpFactory.getStringVlanId(stp.getLabel());
            if (vlanid != null) {
                s.add(vlanid);
            }
        }
        // Need the ability to unset the vlanId so give an unset option.
        s.add(UNSET);
        
        @SuppressWarnings("unchecked")
		final JComboBox choices = new JComboBox(s.toArray());
        
        // Unset the selection.
		choices.setSelectedIndex(-1);
		choices.setBackground(Color.WHITE);
		choices.addActionListener(new ActionListener() {

            // Called when user selects the vlan field.
            @Override
			public void actionPerformed(ActionEvent e) {
				String vlan = (String) choices.getSelectedItem();
                if (vlan == null || UNSET.contentEquals(vlan)) {
                    mVlanId = -1;
                    choices.setSelectedIndex(-1);
                }
                else if (!vlan.isEmpty()) {
                    mVlanId = Integer.parseInt(vlan);
                }
                
                // Now filter the STP based on VLAN.
                sourceStpPanel.removeAll();
                sourceStpPanel.add(getSourceStpSelectionBox());
                sourceStpPanel.updateUI();
                
                destinationStpPanel.removeAll();
                destinationStpPanel.add(getDestinationStpSelectionBox());
                destinationStpPanel.updateUI();
                
				drawShortest();
				repaint();				
			}
		});
		return choices;
	}
    
    private Component getSourceStpSelectionBox() {

        Set<String> s = new TreeSet<>();
        if (mSourceNetwork != null) {
            // Iterate through each STP in the target network.
            for (ResourceRefType stpRef : mSourceNetwork.getStp()) {
                // Determine if the STP has a matching label.
                StpType stp = nsiTopology.getStp(stpRef.getId());
                int vlanId = NsiStpFactory.getVlanId(stp);
                
                if (mVlanId == -1 || mVlanId == vlanId) {
                    s.add(stp.getId());
                }
            }
        }
        
        // Need the ability to unset the vlanId so give an unset option.
        s.add(UNSET);
        
        @SuppressWarnings("unchecked")
		final JComboBox choices = new JComboBox(s.toArray());
        
        // Clear the selection.
		choices.setSelectedIndex(-1);
		choices.setBackground(Color.WHITE);
		choices.addActionListener(new ActionListener() {
            // Called when user selects the vlan field.
            @Override
			public void actionPerformed(ActionEvent e) {
				String stp = (String) choices.getSelectedItem();
                if (stp == null || UNSET.contentEquals(stp)) {
                    mSourceSTP = null;
                    choices.setSelectedIndex(-1);
                }
                else if (!stp.isEmpty()) {
                    mSourceSTP = stp;
                }
                
				drawShortest();
				repaint();				
			}
		});
		return choices;
    }
    
    private Component getDestinationStpSelectionBox() {
        Set<String> s = new TreeSet<String>();
        if (mDestinationNetwork != null) {
            // Iterate through each STP in the target network.
            for (ResourceRefType stpRef : mDestinationNetwork.getStp()) {
                // Determine if the STP has a matching label.
                StpType stp = nsiTopology.getStp(stpRef.getId());
                int vlanId = NsiStpFactory.getVlanId(stp);

                if (mVlanId == -1 || mVlanId == vlanId) {
                    s.add(stp.getId());
                }
            }
        }
        
        // Need the ability to unset the vlanId so give an unset option.
        s.add(UNSET);
        
        @SuppressWarnings("unchecked")
		final JComboBox choices = new JComboBox(s.toArray());
        
        // Clear the selection.
		choices.setSelectedIndex(-1);
		choices.setBackground(Color.WHITE);
		choices.addActionListener(new ActionListener() {
            // Called when user selects the vlan field.
            @Override
			public void actionPerformed(ActionEvent e) {
				String stp = (String) choices.getSelectedItem();
                if (stp == null || UNSET.contentEquals(stp)) {
                    mDestinationSTP = null;
                    choices.setSelectedIndex(-1);
                }
                else if (!stp.isEmpty()) {
                    mDestinationSTP = stp;
                }
                
				drawShortest();
				repaint();				
			}
		});
		return choices;
    }
    
	/**
	 * Finds the shortest path given the path selection criteria.  We have a
     * number of criteria to take into consideration:
     *  - Source and destination vertices as the start and end points of our
     *    computation.  These also act as a filter on the source and destination
     *    STP allowing the user to filter their path selection further.
     *  - VLAN identifier will further filter both the source and destination
     *    STP as well as prune edges from the graph.
     *  - Source and destination STP will fully qualify the request that may be
     *    additionally restricted by the edge VLAN identifier filter.
	 */
	protected void drawShortest() {
        // Empty our verticies and edges for new computation.
        mPred.clear();
        mEdge.clear();
        
        // We need two Netwoks to start path computation.
		if (mSourceNetwork == null || mDestinationNetwork == null) {
			return;
		}
        
        // If no vlan is selected we will need to route for all possible.
        Graph<NetworkVertex, SdpType> tGraph;
        int vlan = mVlanId;
        
        if (vlan == -1) {
            // Check to see if we have STP selected on the ends.
            int sourceVlanId = -1;
            int destnationVlanId = -1;
            if (mSourceSTP != null && !mSourceSTP.isEmpty()) {
                StpType stp = nsiTopology.getStp(mSourceSTP);
                sourceVlanId = NsiStpFactory.getVlanId(stp);
            }
            
            if (mDestinationSTP != null && !mDestinationSTP.isEmpty()) {
                StpType stp = nsiTopology.getStp(mDestinationSTP);
                destnationVlanId = NsiStpFactory.getVlanId(stp);
            }
            
            // No vlan restrictions so we use the whole graph.
            if (sourceVlanId == -1 && destnationVlanId == -1) {
                return;
            }
            
            // When we can support VLAN interchange we can remove this restriction.
            if (sourceVlanId != destnationVlanId) {
                return;
            }
            
            vlan = sourceVlanId;
        }
        
        // Build a temporary graph for out path computation.
        tGraph = new SparseMultigraph<>();

        // Add Networks as verticies.
        for (NetworkVertex network : networkVerticies.values()) {
            tGraph.addVertex(network);
        }        

        // Add bidirectional SDP as edges.
        for (SdpType sdp : nsiTopology.getSdps()) {
            if (sdp.getType() == SdpDirectionalityType.BIDIRECTIONAL) {
                StpType stpA = nsiTopology.getStp(sdp.getDemarcationA().getStp().getId());
                StpType stpZ = nsiTopology.getStp(sdp.getDemarcationZ().getStp().getId());
                int vlanId = NsiStpFactory.getVlanId(stpA);                               
                if (vlanId == vlan) {
                    tGraph.addEdge(sdp,
                        networkVerticies.get(stpA.getNetworkId()),
                        networkVerticies.get(stpZ.getNetworkId()));
                }
            }
        }
        
        DijkstraShortestPath<NetworkVertex, SdpType> alg = new DijkstraShortestPath<>(tGraph);
        
        List<SdpType> path;
        try {
            path = alg.getPath(mSourceNetwork, mDestinationNetwork);
        } catch (Exception ex) {
            log.error("Path computation failed: " + ex);
            throw ex;
        }
        
        for (SdpType sdp : path) {
            StpType stpA = nsiTopology.getStp(sdp.getDemarcationA().getStp().getId());
            StpType stpZ = nsiTopology.getStp(sdp.getDemarcationZ().getStp().getId());
            mPred.add(networkVerticies.get(stpA.getNetworkId()));
            mPred.add(networkVerticies.get(stpZ.getNetworkId()));
            mEdge.add(sdp);
        }
	}

	/**
     * Populates a Sparse Multi-graph with our NSI topology model.
     * 
	 * @return the NSI graph for visualization.
	 */
	private Graph<NetworkVertex, SdpType> getGraph() {
		Graph<NetworkVertex, SdpType> graph = new SparseMultigraph<>();
        
        // Add Networks as verticies.
        for (NetworkVertex network : networkVerticies.values()) {
            graph.addVertex(network);
        }        

        // Add bidirectional SDP as edges.
        for (SdpType sdp : nsiTopology.getSdps()) {
            if (sdp.getType() == SdpDirectionalityType.BIDIRECTIONAL) {
                StpType stpA = nsiTopology.getStp(sdp.getDemarcationA().getStp().getId());
                StpType stpZ = nsiTopology.getStp(sdp.getDemarcationZ().getStp().getId());
                graph.addEdge(sdp,
                        networkVerticies.get(stpA.getNetworkId()),
                        networkVerticies.get(stpZ.getNetworkId()));
            }
        }
        
		return graph;
	}
    
    /**
     * Get a Network object from this topology matching the provided networkId.
     * 
     * @param networkId Identifier for the Network object to retrieve.
     * @return Matching Network object, or null otherwise.
     */
    public NetworkVertex getNetworkByName(String name) {
        for (NetworkVertex network : networkVerticies.values()) {
            if (name.contentEquals(network.getName())) {
                return network;
            }
        }
        
        return null;
    }
    
    /**
     * Main for the TopologyViewer.
     * 
     * @param s Not used.
     * 
     * @throws Exception If there is any issue.
     */
	public static void main(String[] s) throws Exception {
        // Our main GUI frame.
		JFrame jf = new JFrame();
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Pupulate frame with content.
		jf.getContentPane().add(new TopologyViewer("config/"));
		jf.pack();
        
        // Display the frame content.
		jf.setVisible(true);
	}
}
