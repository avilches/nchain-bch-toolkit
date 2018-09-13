 /*
  * @author Alberto Vilches
  * @date 13/09/2018
  */

 import com.nchain.bitcoinkt.net.DnsDiscovery;
 import com.nchain.bitcoinkt.net.discovery.PeerDiscoveryException;
 import com.nchain.params.MainNetParams;
 import com.nchain.params.TestNet3Params;

 import java.net.InetSocketAddress;
 import java.util.concurrent.TimeUnit;

 public class M {

     public static void main(String[] args) {
         try {
//             InetSocketAddress[] addresses = DnsDiscovery.getPeersParallels(TestNet3Params.INSTANCE, 3, TimeUnit.SECONDS);
             InetSocketAddress[] addresses = DnsDiscovery.getPeers(TestNet3Params.INSTANCE);
             for (InetSocketAddress i : addresses) {
                 System.out.println(i.toString());
             }
         } catch (PeerDiscoveryException e) {
             e.printStackTrace();
         }

     }
 }
