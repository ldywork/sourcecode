package learn;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

public class Learn1 {
	public static void main(String[] args) {
		
	}
	@Test
	public void test1() {
		BeanFactory bf = new XmlBeanFactory(new ClassPathResource("text.xml"));
		Object o = bf.getBean("testBean");
		System.out.println(o);
	}
}
