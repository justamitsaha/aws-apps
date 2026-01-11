# EC2 app set up

## AWS CLI

To use AWS CLI need  **Access Key ID** and **Secret Access Key**.  To create your **Access Key ID** and **Secret Access Key**, you need to use the AWS Management Console. It is best practice to create these for an **IAM User** rather than your "Root" (email) account for better security.

### 1. Access the IAM Console
1.  Log in to the [AWS Management Console](https://console.aws.amazon.com/).
2.  In the search bar at the top, type **IAM** and select it from the results.
3.  In the left navigation pane, click on **Users**.


### 2. Select Your User
1.  Find and click on the **Username** you want to create keys for.
2.  If you don't have a user yet, click **Create user**, give it a name, and attach the `AmazonEC2FullAccess`  for EC2


### 3. Generate the Keys
1.  Once inside the specific user's summary page, click the **Security credentials** tab.
2.  Scroll down to the **Access keys** section and click the **Create access key** button.


### 4. Select the Use Case
1.  AWS will ask for your use case. Select **Command Line Interface (CLI)**.
2.  Check the box at the bottom that says _"I understand the above recommendation..."_ and click **Next**.
3.  (Optional) Add a description tag, like "My Laptop CLI Key".
4.  Click **Create access key**.

----------

##  Important: Save Your Keys Immediately

You will now see your **Access Key ID** and your **Secret Access Key**.
-   **The Secret Access Key is only shown ONCE.** * If you close this window without saving it, you cannot recover it; you will have to delete this key and create a new one.
-   **Download the .csv file** and store it in a secure location (like a password manager).
----------

## How to use them on your PC

Now that you have the keys, open your terminal (Command Prompt, PowerShell, or Bash) and run: `aws configure`
It will prompt you for the following:

-   **AWS Access Key ID:** Paste your ID here.
-   **AWS Secret Access Key:** Paste your Secret Key here.
-   **Default region name:** e.g., `us-east-1`
-   **Default output format:**  `json`

## How to run the apps 

- Start the [ec2-start.sh](../2_ec2_s3/ec2-start.sh) script
- It loads the [User Data](./ec2_userdata.sh)
- This loads the [Services](./setup-services.sh)

In case of any issue 

### A. Check cloud-init (MOST IMPORTANT)

`sudo  cat /var/log/cloud-init-output.log` 
Look for:

-   Java install
-   Git clone
-   Maven build
-   `setup-services.sh`
❌ Any error here = apps never started.

----------

### B. Check systemd services

`systemctl status fileReader`

`systemctl status reporting` 

Expected: `Active: active (running)` 

If you see:

-   `Unit not found` → setup-services.sh did not run
-   `failed` → app startup error
    

----------

### C. Check listening ports

`sudo ss -lntp | grep java` 

Expected: `*:8080  *:8081` 

If nothing → apps are not running.

----------

### D. Check logs (deterministic)

`journalctl -u fileReader -n 100 --no-pager`

`journalctl -u reporting -n 100 --no-pager` 

These logs will tell us exactly what failed (DB path, profile, port, etc.).

To tails logs in real-time:
`journalctl -u fileReader -f --no-pager`
`journalctl -u reporting -f --no-pager`
`journalctl -u fileReader -n 100 -f`
`journalctl -u reporting -n 100 -f`





