import java.util.PriorityQueue;

/**
 * Although this class has a history of several years,
 * it is starting from a blank-slate, new and clean implementation
 * as of Fall 2018.
 * <P>
 * Changes include relying solely on a tree for header information
 * and including debug and bits read/written information
 * 
 * @author Owen Astrachan
 */

public class HuffProcessor {

	public static final int BITS_PER_WORD = 8;
	public static final int BITS_PER_INT = 32;
	public static final int ALPH_SIZE = (1 << BITS_PER_WORD); 
	public static final int PSEUDO_EOF = ALPH_SIZE;
	public static final int HUFF_NUMBER = 0xface8200;
	public static final int HUFF_TREE  = HUFF_NUMBER | 1;

	private final int myDebugLevel;
	
	public static final int DEBUG_HIGH = 4;
	public static final int DEBUG_LOW = 1;
	
	public HuffProcessor() {
		this(0);
	}
	
	public HuffProcessor(int debug) {
		myDebugLevel = debug;
	}

	/**
	 * Compresses a file. Process must be reversible and loss-less.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be compressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
/*	public void compress(BitInputStream in, BitOutputStream out){

		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}
		out.close();
	}*/
	
	
	public void compress(BitInputStream in, BitOutputStream out) {
		
		int [] counts = readforCounts(in);
		HuffNode root = makeTreeFromCounts(counts);
		String[] codings = makeCodingsFromTree(root);
		
		out.writeBits(BITS_PER_INT, HUFF_TREE);
		writeHeader(root, out);
		
		in.reset();
		writeCompressedBits(codings, in, out);
		out.close();
		
	}
	
	
	/**
	 * Determines frequencies 
	 * @param in 
	 * @return int array of frequencies of 8-bit characters/chunks
	 */
	private int[] readforCounts(BitInputStream in) {
		
		int [] freqs = new int[ALPH_SIZE +1];
		freqs[PSEUDO_EOF] = 1;
		
		while(true) {
		   int value = in.readBits(BITS_PER_WORD);
		   if (value == -1) break;
		   else {
			   freqs[value] += 1;
		   }
		}
		
		return freqs;
			
	}
	
	/**
	 * PriorityQueue enables greedy algorithm
	 * @param freqs
	 * @return HuffMan Tree/Trie
	 */
	private HuffNode makeTreeFromCounts(int[] freqs) {
		
		PriorityQueue<HuffNode> pq = new PriorityQueue<>();
		
		for(int k = 0; k < freqs.length; k++) {
			if (freqs[k] > 0) { //only add nodes with non-zero weights to pq
				pq.add(new HuffNode(k, freqs[k], null, null));
			}	
		}
		
		while (pq.size() > 1) {
			HuffNode left = pq.remove();
			HuffNode right = pq.remove();
			HuffNode t = new HuffNode(-1, left.myWeight+right.myWeight, left, right); 
			pq.add(t);
		}
		
		HuffNode root = pq.remove();
		return root;
	}
	
	/**
	 * Uses helper method to return String array of bit-stream encodings for characters
	 * @param root
	 * @return
	 */
	private String [] makeCodingsFromTree(HuffNode root) {
		
		String[] encodings = new String[ALPH_SIZE +1];
		codingHelper(root, "", encodings);
		
        return encodings;
	}
	
	/**
	 * Reads tree, creates encoding pathway determined by 0s (left) and 1s (right)
	 * @param root
	 * @param path
	 * @param encodings
	 */
	private void codingHelper(HuffNode root, String path, String[] encodings) { //populates String array encoding

		if(root.myLeft == null && root.myRight == null) {
			encodings[root.myValue] = path;
			return;
		}
		
        codingHelper(root.myLeft, path + "0", encodings); // "0" is added to path when recursive call made on left subtree
    	    codingHelper(root.myRight, path + "1", encodings); // "1" is added to path when recursive call made on right subtree
		
	}
	
    /**
     * If node is internal node, writes single bit of zero
     * If node is leaf, writes a single bit of 1 followed by 9 bits of value stored in the leaf
     * @param root
     * @param out
     */
	private void writeHeader(HuffNode root, BitOutputStream out) {
		
		if(root.myLeft == null && root.myRight == null) {
			out.writeBits(1, 1);
			out.writeBits(BITS_PER_WORD + 1 , root.myValue);
		}
		else {
			out.writeBits(1, 0);
			writeHeader(root.myLeft, out);
			writeHeader(root.myRight, out);
		}
	}
	
	/**
	 * Reads input and uses codings to encode bit-sequence for characters in input
	 * @param codings, String array containing encodings for each character
	 * @param in
	 * @param out
	 */
	private void writeCompressedBits(String [] codings, BitInputStream in, BitOutputStream out) {
		
		in.reset();
		int value = in.readBits(BITS_PER_WORD);
		while(value != -1) {
			String code = codings[value];
			out.writeBits(code.length(), Integer.parseInt(code, 2));	
			value = in.readBits(BITS_PER_WORD);
		}
		
		String codeEOF = codings[PSEUDO_EOF];
		out.writeBits(codeEOF.length(), Integer.parseInt(codeEOF, 2));
				
	}
		
		
	
	/**
	 * Decompresses a file. Output file must be identical bit-by-bit to the
	 * original.
	 *
	 * @param in
	 *            Buffered bit stream of the file to be decompressed.
	 * @param out
	 *            Buffered bit stream writing to the output file.
	 */
/*	public void decompress(BitInputStream in, BitOutputStream out){

		while (true){
			int val = in.readBits(BITS_PER_WORD);
			if (val == -1) break;
			out.writeBits(BITS_PER_WORD, val);
		}
		out.close();
	}*/
	
	public void decompress(BitInputStream in, BitOutputStream out) {
		int bits = in.readBits(BITS_PER_INT);
		if (bits != HUFF_TREE) {
			throw new HuffException("illegal header starts with " + bits);
		}

		
		HuffNode root = readTreeHeader(in);
		readCompressedBits(root,in, out);
		out.close();
		
	}
	
	
	/**
	 * Reads tree, which was stored using recursion
	 * If bit is 0, internal node. Recurses to subtrees.
	 * If bit is 1, leaf. No recursion.
	 * @param in
	 * @return tree
	 */
	private HuffNode readTreeHeader(BitInputStream in) {
		int bit = in.readBits(1);
		if (bit == -1) {
			throw new HuffException("reading bits failed");
		}
		
		if (bit == 0) { //internal node, recursion required
			HuffNode left = readTreeHeader(in);
			HuffNode right = readTreeHeader (in);
			return new HuffNode(0, 0, left, right);
		}
		
		else {
			int value = in.readBits(BITS_PER_WORD + 1);
			return new HuffNode (value, 0, null, null);
		}
	}
	
	/**
	 * Read bits from BitInputStream. Traverses tree from root, going left or right depending on whether a 0 or 1 is read.
	 * @param root
	 * @param in
	 * @param out
	 */
	private void readCompressedBits (HuffNode root, BitInputStream in, BitOutputStream out) {
		
		HuffNode current = root;
		while (true) {
			int bits = in.readBits(1);
			if (bits == -1) {
				throw new HuffException("bad input, no PSEUDO_EOF");
			}
			else {
				if (bits == 0) current = current.myLeft;
				else current = current.myRight;
				
				if(current.myLeft == null && current.myRight == null) {
					if(current.myValue == PSEUDO_EOF) {
						break;  //break out of loop
					}
					else {
						out.writeBits(BITS_PER_WORD, current.myValue);
						current = root; //starts back at root
					}
				}
				
			}
			
		}
		
	}
	
	
	
	
	
	
}