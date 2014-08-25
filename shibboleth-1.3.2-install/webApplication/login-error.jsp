<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html lang="en" xml:lang="en" xmlns="http://www.w3.org/1999/xhtml">
	<head>
		<style type="text/css" title="styled">
			@import url('login.css');
		</style>

		<title>Example Organization WebLogin</title>
	</head>
	<body>	 
		<div id="page">
			<div id="head">
				<h1>Example Organization</h1>
			</div>

			<div id="main">
				<div id="content">
					<p> 
					Please login:
					</p>
					<form method="post" action="j_security_check">
						<table class="login">
							<tr class="login">
								<td colspan="3" class="login"><span class="errortext">Incorrect Username or Password.  Please try again.</span></td>
							</tr>
							<tr class="login">
								<td class="login"><strong>UID</strong></td>
								<td class="login"><input name="j_username" type="text" id="j_username" size="16" /></td></tr>
							<tr class="login">
								<td class="login">
									<strong>Password</strong>
								</td>

								<td class="login">
									<input name="j_password" type= "password" id="j_password" size= "16" /></td>
								<td class="login">
									<input name="Login" type="submit" id="Login" value="Login" />
								</td>
							</tr>
						</table>
					</form>

				</div>
				<div id="helptext">
					<p>
					The resource that you have attempted to access requires that you log in with your with your Example Organization UID.
					</p>
				</div>

			</div>
		</div>
	</body>
</html>
