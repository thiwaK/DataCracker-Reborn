from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.common.desired_capabilities import DesiredCapabilities


print("Done importing modules")

options = webdriver.ChromeOptions()
options.add_experimental_option('androidPackage', 'com.android.chrome')
driver = webdriver.Chrome()
driver.get('https://google.com')
driver.quit()