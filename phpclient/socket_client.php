<?php
header("Content-type:text/html;charset=utf-8");

error_reporting(E_ALL & ~E_NOTICE);

class Common_model {

	public static function is_big_endian()
	{
		$bin = pack("L", 0x12345678);
		$hex = bin2hex($bin);
		if (ord(pack("H2", $hex)) === 0x78)
		{
			return FALSE;
		}
		return TRUE;
	}

	public static function my_pack($num)
	{
		$bin     = "";
		$padding = 0;
	
		if ($num >= 0x00 && $num <= 0xFF)
		{
			$padding = str_repeat(chr(0), 3);
	
			if ( static::is_big_endian())
			{
				$bin = $padding . chr($num);
			}
			else
			{
				$bin = $padding . chr($num);
			}
		}
		else if ($num > 0xFF && $num <= 0xFFFF)
		{
			$padding = str_repeat(chr(0), 2);
			$byte3   = ($num >> 8) & 0xFF;
			$byte4   = $num & 0xFF;
			
			if ( static::is_big_endian())
			{
				$bin = $padding . chr($byte3) . chr($byte4);
			}
			else
			{
				$bin = $padding . chr($byte3) . chr($byte4);
			}
		}
		else if ($num > 0xFFFF && $num <= 0x7FFFFF)
		{
			$padding = chr(0);
			$byte2   = ($num >> 16) & 0xFF;
			$byte3   = ($num >> 8) & 0xFF;
			$byte4   = $num & 0xFF;

			if ( static::is_big_endian())
			{
				$bin = $padding . chr($byte2) . chr($byte3) . chr($byte4);
			}
			else
			{
				$bin = $padding . chr($byte2) . chr($byte3) . chr($byte4);
			}
		}
		else
		{
			$byte1 = ($num >> 24) & 0xFF;
			$byte2 = ($num >> 16) & 0xFF;
			$byte3 = ($num >> 8) & 0xFF;
			$byte4 = $num & 0xFF;
	
			if ( static::is_big_endian())
			{
				$bin = chr($byte1) . chr($byte2) . chr($byte3) . chr($byte4);
			}
			else
			{
				$bin = chr($byte1) . chr($byte2) . chr($byte3) . chr($byte4);
			}
		}
		var_dump(bin2hex($bin));
		return $bin;
	}

	public static function my_unpack($bin)
	{
		$byte1 = ord($bin{0});
		$byte2 = ord($bin{1});
		$byte3 = ord($bin{2});
		$byte4 = ord($bin{3});
	
		if (static::is_big_endian())
		{
			$num = ($byte1  << 24) | ($byte2 << 16) | ($byte3 << 8) | $byte4;
		}
		else
		{
			$num = ($byte4  << 24) | ($byte3 << 16) | ($byte2 << 8) | $byte1;
		}

		return array($num);
	}

}

if (isset($_POST['command'])){

	$type = $_POST['type'];
	$service_port = $_POST['service_port'];
	$address = $_POST['address'];
	$content = $_POST['content'];

	$xml_l = '';
	$in  = '';

	$xml_l	= 
		"<?xml version='1.0' encoding='utf-8'?>".
		"<package><body>".
		"<type>".$type."</type>".
		"<content>".$content."</content>".
		"<parentid>0</parentid>".
		"<selfgroupid>0</selfgroupid>".
		"<selfuserno>0</selfuserno>".
		"<peergroupid>0</peergroupid>".
		"<peeruserno>0</peeruserno>".
		"</body></package>";


	$xml_l_len = strlen($xml_l);
	//echo "len:$xml_l_len<br>";

	$xml_l_len_str = strval($xml_l_len);
	//echo "len str:$xml_l_len_str<br>";

	$intlen = Common_model::my_pack($xml_l_len);
	
	$xml = $intlen . $xml_l;

	//创建tcp/ip socket
	$socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
	if ($socket < 0){

		echo "socket创建失败原因：" . socket_strerror($socket) . "\n";
	}else{

		echo "OK, HH.\n";
	}

	//设置超时
	socket_set_option($socket, SOL_SOCKET, SO_RCVTIMEO, array("sec"=>10,"usec"=>0));
	socket_set_option($socket, SOL_SOCKET, SO_SNDTIMEO, array("sec"=>10,"usec"=>0));

	//连接
	$connection = socket_connect($socket, $address, $service_port);

	socket_write($socket, $xml) or die("Write failed\n");

	echo "OK.<br>";
	echo "Reading Backinformation:\n\n";

	$buffer = '';

	//头四个字节表示报文长度
	$in = socket_read($socket, 4);
	$num = hexdec(bin2hex($in));
	echo $num . "<br>";
	//$in_len = intval($in);
	
	echo "<br>num:" . $num . "<br>";
	
	//下面这种写法会被阻塞
	// while($in = @socket_read($socket, $in_len)){

	// 	if (!$in)break;
	// 	$buffer .= $in;
	// 	if (substr($buffer, -2)=='\n')break;
	// }

	if (false !== ($rets = socket_recv($socket, $buffer, $num, MSG_WAITALL))){

		echo "Read $rets bytes from socket_recv(). Closing socket...\n";
	}else{

		echo "socket_recv() failed; reason: " . socket_strerror(socket_last_error($socket)) . "\n";
	}

	socket_close($socket);

	echo "ok,hehe.\n\n";

	echo "<xmp>";
	echo $buffer;
	echo "</xmp>";
	echo "<br>";
}
?>
<html>
	<title>socket test</title>
	<style type="text/css">
		input{
			position: absolute;
			left: 140px;
		}
		span{

			position: absolute;
			left: 340px;
		}
	</style>
<body>
<form action="" method="post">

	type:		<input type="text" name="type" value="1000" /><span>1000表示推送消息,可以修改服务器代码，添加更多命令</span><br>
	address:	<input type="text" name="address" value="192.168.8.119" /><br>
	port:   	<input type="text" name="service_port" value="9999" /><br>
	content:	<input type="text" name="content" value="come from php test" /><br><br>

	<input type="submit" value="Submit"/>
	<input type="hidden" value="1" name="command"/>
</form>
</body>
</html>