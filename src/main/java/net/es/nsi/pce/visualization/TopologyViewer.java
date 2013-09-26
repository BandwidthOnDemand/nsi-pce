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
import javax.swing.SwingConstants;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.shortestpath.BFSDistanceLabeler;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import java.io.File;
import net.es.nsi.pce.config.topo.XmlTopologyProvider;
import net.es.nsi.pce.config.topo.nml.Directionality;
import net.es.nsi.pce.pf.api.topo.Network;
import net.es.nsi.pce.pf.api.topo.Sdp;
import net.es.nsi.pce.pf.api.topo.Stp;
import net.es.nsi.pce.pf.api.topo.Topology;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Demonstrates use of the shortest path algorithm and visualization of the
 * results.
 * 
 * @author danyelf
 */
public class TopologyViewer extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7526217664458188502L;

	/**
	 * Starting vertex
	 */
	private Network mFrom;

	/**
	 * Ending vertex
	 */	
	private Network mTo;
    
    // VLAN id for path finding.
    private int vlanId;
    
	private Graph<Network,Sdp> mGraph;
	private Set<Network> mPred;
	private Topology topology;
    
	public TopologyViewer() {
        // Configure path to log4j configuration.
        String log4jConfig = new StringBuilder("config/").append("log4j.xml").toString().replace("/", File.separator);
        
        // Load and watch the log4j configuration file for changes.
        DOMConfigurator.configureAndWatch(log4jConfig, 45 * 1000);
        
        // Load NSI topology.
        XmlTopologyProvider provider = new XmlTopologyProvider();
        provider.setTopologySource("config/topology/");
        try {
            provider.loadTopology();
        }
        catch (Exception ex) {
            System.err.println("loadTopology() Failed: ");
            ex.printStackTrace();
        }
        
        topology = provider.getTopology();
        
		this.mGraph = getGraph();
		setBackground(Color.WHITE);
		// show graph
        final Layout<Network, Sdp> layout = new FRLayout<Network, Sdp>(mGraph);
        final VisualizationViewer<Network, Sdp> vv = new VisualizationViewer<Network, Sdp>(layout);
        vv.setBackground(Color.WHITE);

        vv.getRenderContext().setVertexDrawPaintTransformer(new MyVertexDrawPaintFunction<Network>());
        vv.getRenderContext().setVertexFillPaintTransformer(new MyVertexFillPaintFunction<Network>());
        vv.getRenderContext().setEdgeDrawPaintTransformer(new MyEdgePaintFunction());
        vv.getRenderContext().setEdgeStrokeTransformer(new MyEdgeStrokeFunction());
        vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller<Network>());
        vv.setGraphMouse(new DefaultModalGraphMouse<Network, Sdp>());
        
        vv.addPostRenderPaintable(new VisualizationViewer.Paintable(){
            
            public boolean useTransform() {
                return true;
            }
            public void paint(Graphics g) {
                if(mPred == null) return;
                
                // for all edges, paint edges that are in shortest path
                for (Sdp e : layout.getGraph().getEdges()) {
                    
                    if(isBlessed(e)) {
                        Network v1 = mGraph.getEndpoints(e).getFirst();
                        Network v2 = mGraph.getEndpoints(e).getSecond();
                        Point2D p1 = layout.transform(v1);
                        Point2D p2 = layout.transform(v2);
                        p1 = vv.getRenderContext().getMultiLayerTransformer().transform(Layer.LAYOUT, p1);
                        p2 = vv.getRenderContext().getMultiLayerTransformer().transform(Layer.LAYOUT, p2);
                        Renderer<Network, Sdp> renderer = vv.getRenderer();
                        renderer.renderEdge(
                                vv.getRenderContext(),
                                layout,
                                e);
                    }
                }
            }
        });
        
        setLayout(new BorderLayout());
        add(vv, BorderLayout.CENTER);
        // set up controls
        add(setUpControls(), BorderLayout.SOUTH);
	}

    boolean isBlessed( Sdp e ) {
    	Pair<Network> endpoints = mGraph.getEndpoints(e);
		Network v1= endpoints.getFirst()	;
		Network v2= endpoints.getSecond() ;
		return v1.equals(v2) == false && mPred.contains(v1) && mPred.contains(v2);
    }
    
	/**
	 * @author danyelf
	 */
	public class MyEdgePaintFunction implements Transformer<Sdp, Paint> {
	    
        @Override
		public Paint transform(Sdp e) {
			if ( mPred == null || mPred.isEmpty()) return Color.BLACK;
			if( isBlessed( e )) {
				return new Color(0.0f, 0.0f, 1.0f, 0.5f); //Color.BLUE;
			} else {
				return Color.LIGHT_GRAY;
			}
		}
	}
	
	public class MyEdgeStrokeFunction implements Transformer<Sdp,Stroke> {
        protected final Stroke THIN = new BasicStroke(1);
        protected final Stroke THICK = new BasicStroke(1);

        @Override
        public Stroke transform(Sdp e) {
			if ( mPred == null || mPred.isEmpty()) return THIN;
			if (isBlessed( e ) ) {
			    return THICK;
			} else 
			    return THIN;
        }
	    
	}
	
	public class MyVertexDrawPaintFunction<V> implements Transformer<V,Paint> {

        @Override
		public Paint transform(V v) {
			return Color.black;
		}

	}

	public class MyVertexFillPaintFunction<V> implements Transformer<V,Paint> {

        @Override
		public Paint transform( V v ) {
			if ( v == mFrom) {
				return Color.BLUE;
			}
			if ( v == mTo ) {
				return Color.BLUE;
			}
			if ( mPred == null ) {
				return Color.LIGHT_GRAY;
			} else {
				if ( mPred.contains(v)) {
					return Color.RED;
				} else {
					return Color.LIGHT_GRAY;
				}
			}
		}

	}

	/**
	 *  
	 */
	private JPanel setUpControls() {
		JPanel jp = new JPanel();
		jp.setBackground(Color.WHITE);
		jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));
		jp.setBorder(BorderFactory.createLineBorder(Color.black, 3));		
		jp.add(
			new JLabel("Select a pair of vertices for which a shortest path will be displayed"));
		JPanel jp2 = new JPanel();
		jp2.add(new JLabel("From network", SwingConstants.LEFT));
		jp2.add(getSelectionBox(true));
		jp2.setBackground(Color.white);
		JPanel jp3 = new JPanel();
		jp3.add(new JLabel("To network", SwingConstants.LEFT));
		jp3.add(getSelectionBox(false));
		jp3.setBackground(Color.white);
        JPanel jp4 = new JPanel();
		jp4.add(new JLabel("VLAN", SwingConstants.LEFT));
		jp4.add(getVlanIdBox());
		jp4.setBackground(Color.white);
		jp.add( jp2 );
		jp.add( jp3 );
        jp.add( jp4 );
		return jp;
	}

	private Component getSelectionBox(final boolean from) {

		Set<String> s = new TreeSet<String>();
		
		for (Network v : mGraph.getVertices()) {
			s.add(v.getNetworkId());
		}
        
        // TODO: Build list of vertex (network as strings).
		final JComboBox choices = new JComboBox(s.toArray());
		choices.setSelectedIndex(-1);
		choices.setBackground(Color.WHITE);
		choices.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String v = (String)choices.getSelectedItem();
					
				if (from) {
					mFrom = topology.getNetwork(v);
				} else {
					mTo = topology.getNetwork(v);
				}
				drawShortest();
				repaint();				
			}
		});
		return choices;
	}

    private Component getVlanIdBox() {

        Set<String> s = new TreeSet<String>();
                
        for (Network v : mGraph.getVertices()) {
            for (Stp stp : v.getStps()) {
                s.add(Integer.toString(stp.getVlanId()));
            }
        }
        
        // TODO: Build list of vertex (network as strings).
		final JComboBox choices = new JComboBox(s.toArray());
		choices.setSelectedIndex(-1);
		choices.setBackground(Color.WHITE);
		choices.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				String vlan = (String) choices.getSelectedItem();
                if (vlan != null && !vlan.isEmpty()) {
                    vlanId = Integer.parseInt(vlan);
                }
				drawShortest();
				repaint();				
			}
		});
		return choices;
	}
        
	/**
	 *  
	 */
	protected void drawShortest() {
		if (mFrom == null || mTo == null) {
			return;
		}
		BFSDistanceLabeler<Network, Sdp> bdl = new BFSDistanceLabeler<Network, Sdp>();
		bdl.labelDistances(mGraph, mFrom);
		mPred = new HashSet<Network>();
		
		// grab a predecessor
		Network v = mTo;
		Set<Network> prd = bdl.getPredecessors(v);
		mPred.add( mTo );
		while( prd != null && prd.size() > 0) {
			v = prd.iterator().next();
			mPred.add( v );
			if ( v == mFrom ) return;
			prd = bdl.getPredecessors(v);
		}
	}

	public static void main(String[] s) {
		JFrame jf = new JFrame();
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.getContentPane().add(new TopologyViewer());
		jf.pack();
		jf.setVisible(true);
	}

	/**
	 * @return the graph for this demo
	 */
	Graph<Network, Sdp> getGraph() {
        
		Graph<Network, Sdp> graph = new SparseMultigraph<Network, Sdp>();
        
        // Add Networks as verticies.
        for (Network network : topology.getNetworks()) {
            System.out.println("Adding Vertex: " + network.getNetworkId());
            graph.addVertex(network);
        }        

        // Add bidirectional SDP as edges.
        for (Sdp sdp : topology.getSdps()) {
            if (sdp.getDirectionality() == Directionality.bidirectional) {
                System.out.println("Adding bidirectional edge: " + sdp.getId());
                graph.addEdge(sdp, sdp.getA().getNetwork(), sdp.getZ().getNetwork());
            }
        }
        
		return graph;
	}

}
